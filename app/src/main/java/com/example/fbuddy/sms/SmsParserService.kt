package com.example.fbuddy.sms

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.util.Log
import com.example.fbuddy.data.db.FBuddyDatabase
import com.example.fbuddy.data.db.TransactionEntity
import com.example.fbuddy.data.model.TransactionSource
import com.example.fbuddy.data.model.TransactionType
import com.example.fbuddy.utils.Categorizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class SmsParserService : Service() {

    companion object {
        private const val TAG = "SmsParserService"
        const val ACTION_FULL_SCAN    = "com.example.fbuddy.FULL_SCAN"
        const val ACTION_PARSE_SINGLE = "com.example.fbuddy.PARSE_SINGLE"
        const val EXTRA_BODY          = "body"
        const val EXTRA_SMS_ID        = "sms_id"
        const val EXTRA_DATE          = "date"

        // ── Keywords that identify bank SMS ──────────────────────────────
        private val BANK_KEYWORDS = listOf(
            "debited", "credited", "transaction", "a/c", "upi",
            "neft", "imps", "balance", "inr", "rs.", "₹",
            "sbi", "hdfc", "icici", "axis", "kotak"
        )

        // ── Debit regex patterns ──────────────────────────────────────────
        private val DEBIT_PATTERNS = listOf(
            // "debited by Rs. 450.00" / "debited by Rs 450"
            Pattern.compile(
                """(?:debited|deducted|spent|paid)\s*(?:by|of|from|at|for)?\s*(?:rs\.?|inr|₹)\s*([\d,]+(?:\.\d{1,2})?)""",
                Pattern.CASE_INSENSITIVE
            ),
            // "INR 450.00 debited"
            Pattern.compile(
                """(?:rs\.?|inr|₹)\s*([\d,]+(?:\.\d{1,2})?)\s*(?:debited|deducted|spent|paid)""",
                Pattern.CASE_INSENSITIVE
            ),
            // "Rs.450.00 spent on your card"
            Pattern.compile(
                """(?:rs\.?|inr|₹)([\d,]+(?:\.\d{1,2})?)""",
                Pattern.CASE_INSENSITIVE
            ),
        )

        // ── Credit regex patterns ─────────────────────────────────────────
        private val CREDIT_PATTERNS = listOf(
            Pattern.compile(
                """(?:credited|received|deposited)\s*(?:with|by|of)?\s*(?:rs\.?|inr|₹)\s*([\d,]+(?:\.\d{1,2})?)""",
                Pattern.CASE_INSENSITIVE
            ),
            Pattern.compile(
                """(?:rs\.?|inr|₹)\s*([\d,]+(?:\.\d{1,2})?)\s*(?:credited|received|deposited)""",
                Pattern.CASE_INSENSITIVE
            ),
        )

        // ── Merchant pattern ──────────────────────────────────────────────
        private val MERCHANT_PATTERN = Pattern.compile(
            """(?:for|at|to|towards)\s+([A-Za-z0-9][A-Za-z0-9 &'\-]{1,40}?)(?:\s+on\s|\s+ref|\s+txn|\s*\.|$)""",
            Pattern.CASE_INSENSITIVE
        )

        fun startFullScan(context: Context) {
            try {
                val intent = Intent(context, SmsParserService::class.java).apply {
                    action = ACTION_FULL_SCAN
                }
                context.startService(intent)
                Log.d(TAG, "Full scan service started")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start full scan: ${e.message}", e)
            }
        }

        fun parseSingleSms(context: Context, body: String, smsId: Long, date: Long) {
            try {
                val intent = Intent(context, SmsParserService::class.java).apply {
                    action = ACTION_PARSE_SINGLE
                    putExtra(EXTRA_BODY, body)
                    putExtra(EXTRA_SMS_ID, smsId)
                    putExtra(EXTRA_DATE, date)
                }
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start parse single: ${e.message}", e)
            }
        }

        fun isBankSms(body: String): Boolean {
            val lower = body.lowercase()
            return BANK_KEYWORDS.any { lower.contains(it) }
        }

        fun parseTransaction(
            body: String,
            smsId: Long? = null,
            dateMillis: Long = System.currentTimeMillis()
        ): TransactionEntity? {
            return try {
                if (!isBankSms(body)) return null

                var amount: Double? = null
                var type: TransactionType? = null

                // Try DEBIT first
                for (pattern in DEBIT_PATTERNS) {
                    val m = pattern.matcher(body)
                    if (m.find()) {
                        val raw = m.group(1)?.replace(",", "")
                        val v = raw?.toDoubleOrNull()
                        if (v != null && v > 0) {
                            // Only mark as debit if body actually contains debit word
                            val lower = body.lowercase()
                            if (lower.contains("debit") || lower.contains("spent") ||
                                lower.contains("paid") || lower.contains("deducted")) {
                                amount = v
                                type = TransactionType.DEBIT
                                break
                            }
                        }
                    }
                }

                // Try CREDIT
                if (type == null) {
                    for (pattern in CREDIT_PATTERNS) {
                        val m = pattern.matcher(body)
                        if (m.find()) {
                            val raw = m.group(1)?.replace(",", "")
                            val v = raw?.toDoubleOrNull()
                            if (v != null && v > 0) {
                                amount = v
                                type = TransactionType.CREDIT
                                break
                            }
                        }
                    }
                }

                // Fallback: if still no type, try the generic amount pattern
                if (type == null) {
                    val lower = body.lowercase()
                    for (pattern in DEBIT_PATTERNS) {
                        val m = pattern.matcher(body)
                        if (m.find()) {
                            val raw = m.group(1)?.replace(",", "")
                            val v = raw?.toDoubleOrNull()
                            if (v != null && v > 0) {
                                amount = v
                                type = if (lower.contains("credit")) TransactionType.CREDIT
                                else TransactionType.DEBIT
                                break
                            }
                        }
                    }
                }

                if (amount == null || type == null) {
                    Log.w(TAG, "Could not parse: ${body.take(80)}")
                    return null
                }

                // Extract merchant
                val merchantMatcher = MERCHANT_PATTERN.matcher(body)
                val merchant = if (merchantMatcher.find()) {
                    merchantMatcher.group(1)?.trim()
                        ?.replace(Regex("""(?i)\s*(on|ref|txn)\s.*"""), "")
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
                } else null

                // Detect bank
                val lower = body.lowercase()
                val bankName = listOf("sbi","hdfc","icici","axis","kotak","pnb","bob")
                    .firstOrNull { lower.contains(it) }?.uppercase()

                // Account last 4
                val accRegex = Regex("""[xX*]{2,6}(\d{4})""")
                val accountLast4 = accRegex.find(body)?.groupValues?.get(1)

                val category = Categorizer.categorize(merchant, body)

                Log.d(TAG, "✅ Parsed: amount=₹$amount type=$type merchant=$merchant category=$category")

                TransactionEntity(
                    amount       = amount,
                    type         = type,
                    merchant     = merchant,
                    category     = category,
                    timestamp    = dateMillis,
                    source       = TransactionSource.SMS,
                    rawText      = body,
                    bankName     = bankName,
                    accountLast4 = accountLast4,
                    smsMessageId = smsId
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing SMS: ${e.message}", e)
                null
            }
        }
    }

    // ── Service lifecycle ────────────────────────────────────────────────
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_FULL_SCAN    -> serviceScope.launch { performFullScan() }
            ACTION_PARSE_SINGLE -> {
                val body  = intent.getStringExtra(EXTRA_BODY) ?: return START_NOT_STICKY
                val smsId = intent.getLongExtra(EXTRA_SMS_ID, -1L).takeIf { it >= 0 }
                val date  = intent.getLongExtra(EXTRA_DATE, System.currentTimeMillis())
                serviceScope.launch { parseSingleAndSave(body, smsId, date) }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    // ── Full inbox scan ───────────────────────────────────────────────────
    private suspend fun performFullScan() {
        Log.d(TAG, "Starting full SMS scan…")
        val db  = FBuddyDatabase.getInstance(applicationContext)
        val dao = db.transactionDao()
        var parsed = 0
        var saved  = 0

        try {
            val uri = Uri.parse("content://sms/inbox")
            val projection = arrayOf("_id", "body", "date")
            val cursor = contentResolver.query(uri, projection, null, null, "date DESC")
                ?: run { Log.e(TAG, "Cursor is null — READ_SMS missing?"); return }

            Log.d(TAG, "Inbox has ${cursor.count} total messages")

            cursor.use { c ->
                val idCol   = c.getColumnIndexOrThrow("_id")
                val bodyCol = c.getColumnIndexOrThrow("body")
                val dateCol = c.getColumnIndexOrThrow("date")

                while (c.moveToNext()) {
                    val smsId = c.getLong(idCol)
                    val body  = c.getString(bodyCol) ?: continue
                    val date  = c.getLong(dateCol)

                    val txn = parseTransaction(body, smsId, date) ?: continue
                    parsed++

                    val existing = dao.findBySmsMessageId(smsId)
                    if (existing == null) {
                        dao.upsert(txn)
                        saved++
                        Log.d(TAG, "Saved: ${txn.merchant} ₹${txn.amount}")
                    }
                }
            }

            Log.d(TAG, "Full scan complete: parsed=$parsed saved=$saved")

        } catch (e: Exception) {
            Log.e(TAG, "Error during full scan: ${e.message}", e)
        }
    }

    // ── Parse and save a single live SMS ─────────────────────────────────
    private suspend fun parseSingleAndSave(body: String, smsId: Long?, date: Long) {
        try {
            val txn = parseTransaction(body, smsId, date) ?: return
            val db  = FBuddyDatabase.getInstance(applicationContext)
            val dao = db.transactionDao()

            // Avoid duplicates
            val existing = smsId?.let { dao.findBySmsMessageId(it) }
            if (existing == null) {
                dao.upsert(txn)
                Log.d(TAG, "Live SMS saved: ${txn.merchant} ₹${txn.amount}")
            } else {
                Log.d(TAG, "Duplicate skipped for smsId=$smsId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving single SMS: ${e.message}", e)
        }
    }
}
