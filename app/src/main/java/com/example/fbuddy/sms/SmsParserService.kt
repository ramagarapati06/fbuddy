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

        // ── Strict bank sender identification ────────────────────────────
        // Only process SMS from known bank sender IDs (not random numbers)
        private val BANK_SENDER_PATTERNS = listOf(
            // Indian bank short codes (alphanumeric senders)
            Regex("""^[A-Z]{2}-[A-Z]{0,8}(SBI|HDFC|ICICI|AXIS|KOTAK|PNB|BOB|CANARA|UNION|IDBI|YES|INDUS|FEDERAL|BANDHAN|RBL|PAYTM|AIRTEL|JIO|UPI|BHIM|GPAY|PHONEPE|AMAZON|FLIPKART|NEFT|IMPS|NACH)""", RegexOption.IGNORE_CASE),
            Regex("""^(SBI|HDFC|ICICI|AXIS|KOTAK|PNB|BOB|CANARA|UNION|IDBI|YESBNK|INDUS|FEDERAL|BANDHAN|RBL|PAYTM|ALERTS|VERIFY|SECURE)""", RegexOption.IGNORE_CASE),
        )

        // ── Keywords that MUST be present for a valid bank transaction ────
        // At least ONE of these must appear for us to parse it
        private val REQUIRED_TRANSACTION_KEYWORDS = listOf(
            "debited", "credited", "debit", "credit",
            "spent", "paid", "payment", "transaction",
            "transferred", "transfer", "withdrawn", "withdrawal",
            "a/c", "acct", "account",
            "upi", "neft", "imps", "rtgs",
            "rs.", "rs ", "inr", "₹",
        )

        // ── Spam/OTP/promotional filters — if ANY of these appear, SKIP ──
        private val SPAM_KEYWORDS = listOf(
            "otp", "one time password", "verification code", "verify",
            "login", "sign in", "password", "pin",
            "offer", "discount", "cashback offer", "deal", "sale",
            "congratulations", "winner", "prize", "lucky",
            "click here", "visit", "download", "install",
            "loan offer", "pre-approved", "apply now",
            "subscribe", "unsubscribe",
            "your order", "order placed", "order confirmed", "delivery",
            "recharge", "data pack", "validity",
        )

        // ── Amount extraction patterns ────────────────────────────────────
        // Ordered from most specific to least specific
        private val DEBIT_PATTERNS = listOf(
            // "debited by Rs.450" / "debited by Rs 450.00"
            Pattern.compile(
                """(?:debited|deducted)\s*(?:by|from|for|of|with)?\s*(?:rs\.?|inr|₹)\s*([\d,]+(?:\.\d{1,2})?)""",
                Pattern.CASE_INSENSITIVE
            ),
            // "Rs.450 debited" / "INR 450 debited"
            Pattern.compile(
                """(?:rs\.?|inr|₹)\s*([\d,]+(?:\.\d{1,2})?)\s*(?:has been\s*)?(?:debited|deducted|spent|paid)""",
                Pattern.CASE_INSENSITIVE
            ),
            // "spent Rs.450" / "paid Rs.450"
            Pattern.compile(
                """(?:spent|paid|payment\s+of)\s*(?:rs\.?|inr|₹)\s*([\d,]+(?:\.\d{1,2})?)""",
                Pattern.CASE_INSENSITIVE
            ),
            // "Rs.450 spent on your card"
            Pattern.compile(
                """(?:rs\.?|inr|₹)([\d,]+(?:\.\d{1,2})?)\s+(?:spent|paid|debited|deducted)""",
                Pattern.CASE_INSENSITIVE
            ),
        )

        private val CREDIT_PATTERNS = listOf(
            Pattern.compile(
                """(?:credited|received|deposited)\s*(?:with|by|of|to)?\s*(?:rs\.?|inr|₹)\s*([\d,]+(?:\.\d{1,2})?)""",
                Pattern.CASE_INSENSITIVE
            ),
            Pattern.compile(
                """(?:rs\.?|inr|₹)\s*([\d,]+(?:\.\d{1,2})?)\s*(?:has been\s*)?(?:credited|received|deposited)""",
                Pattern.CASE_INSENSITIVE
            ),
        )

        private val MERCHANT_PATTERN = Pattern.compile(
            """(?:at|to|from|for|towards|merchant[:\s]+)\s*([A-Za-z0-9][A-Za-z0-9 &'.\-]{1,35}?)(?:\s+on\b|\s+ref\b|\s+txn\b|\s+via\b|\s*[,.]|$)""",
            Pattern.CASE_INSENSITIVE
        )

        private val ACCOUNT_PATTERN = Pattern.compile(
            """(?:a/c|acct?|account|card)\s*(?:no\.?|num(?:ber)?)?\s*[xX*]{0,8}(\d{4})""",
            Pattern.CASE_INSENSITIVE
        )

        // ── Public helpers ────────────────────────────────────────────────

        fun startFullScan(context: Context) {
            try {
                context.startService(
                    Intent(context, SmsParserService::class.java).apply { action = ACTION_FULL_SCAN }
                )
            } catch (e: Exception) {
                Log.e(TAG, "startFullScan failed: ${e.message}")
            }
        }

        fun parseSingleSms(context: Context, body: String, smsId: Long, date: Long) {
            try {
                context.startService(
                    Intent(context, SmsParserService::class.java).apply {
                        action = ACTION_PARSE_SINGLE
                        putExtra(EXTRA_BODY, body)
                        putExtra(EXTRA_SMS_ID, smsId)
                        putExtra(EXTRA_DATE, date)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "parseSingleSms failed: ${e.message}")
            }
        }

        /**
         * Returns true only if this SMS looks like a genuine bank transaction.
         * Filters out OTPs, spam, promos, and non-financial messages.
         */
        fun isBankSms(body: String): Boolean {
            val lower = body.lowercase()

            // Must contain at least one transaction keyword
            val hasTransactionKeyword = REQUIRED_TRANSACTION_KEYWORDS.any { lower.contains(it) }
            if (!hasTransactionKeyword) return false

            // Must NOT be spam/OTP/promo
            val isSpam = SPAM_KEYWORDS.any { lower.contains(it) }
            if (isSpam) return false

            // Must contain an amount pattern
            val hasAmount = lower.contains(Regex("""(?:rs\.?|inr|₹)\s*[\d,]+"""))
            if (!hasAmount) return false

            return true
        }

        /**
         * Checks if a sender ID looks like a bank/financial institution.
         * Returns true for alphanumeric sender IDs (genuine bank SMS),
         * false for regular 10-digit phone numbers (personal SMS).
         */
        fun isBankSender(address: String?): Boolean {
            if (address == null) return false
            val trimmed = address.trim()

            // Regular 10-digit phone numbers are NOT banks
            if (trimmed.matches(Regex("""^\+?[0-9]{10,13}$"""))) return false

            // Short codes like VM-SBIBNK, AX-HDFCBK, etc. — these are banks
            return true
        }

        fun parseTransaction(
            body: String,
            smsId: Long? = null,
            dateMillis: Long = System.currentTimeMillis()
        ): TransactionEntity? {
            return try {
                if (!isBankSms(body)) {
                    Log.d(TAG, "Skipped (not a bank SMS): ${body.take(50)}")
                    return null
                }

                val lower = body.lowercase()
                var amount: Double? = null
                var type: TransactionType? = null

                // Try DEBIT patterns
                for (pattern in DEBIT_PATTERNS) {
                    val m = pattern.matcher(body)
                    if (m.find()) {
                        val v = m.group(1)?.replace(",", "")?.toDoubleOrNull()
                        if (v != null && v > 0) {
                            // Double-check body actually says debit/spent/paid
                            if (lower.contains("debit") || lower.contains("spent") ||
                                lower.contains("paid") || lower.contains("deducted") ||
                                lower.contains("payment")) {
                                amount = v
                                type = TransactionType.DEBIT
                                break
                            }
                        }
                    }
                }

                // Try CREDIT patterns
                if (type == null) {
                    for (pattern in CREDIT_PATTERNS) {
                        val m = pattern.matcher(body)
                        if (m.find()) {
                            val v = m.group(1)?.replace(",", "")?.toDoubleOrNull()
                            if (v != null && v > 0) {
                                if (lower.contains("credit") || lower.contains("received") ||
                                    lower.contains("deposited")) {
                                    amount = v
                                    type = TransactionType.CREDIT
                                    break
                                }
                            }
                        }
                    }
                }

                if (amount == null || type == null || amount < 1.0) {
                    Log.d(TAG, "Could not parse amount/type: ${body.take(60)}")
                    return null
                }

                // Extract merchant
                val merchantMatcher = MERCHANT_PATTERN.matcher(body)
                val merchant = if (merchantMatcher.find()) {
                    merchantMatcher.group(1)?.trim()
                        ?.replace(Regex("""(?i)\s*(on|ref|txn|via)\s.*"""), "")
                        ?.trim()
                        ?.takeIf { it.length >= 2 && !it.matches(Regex("""[0-9]+""")) }
                } else null

                // Detect bank name
                val bankName = listOf("sbi","hdfc","icici","axis","kotak","pnb","bob","canara","yes","indus","federal","rbl")
                    .firstOrNull { lower.contains(it) }?.uppercase()

                // Account last 4
                val accountLast4 = ACCOUNT_PATTERN.matcher(body).let { m ->
                    if (m.find()) m.group(1) else null
                }

                val category = Categorizer.categorize(merchant, body)

                Log.d(TAG, "✅ Parsed: ₹$amount $type | merchant=$merchant | category=$category")

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
                Log.e(TAG, "Parse error: ${e.message}", e)
                null
            }
        }
    }

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

    private suspend fun performFullScan() {
        Log.d(TAG, "Starting full SMS scan…")
        val dao = FBuddyDatabase.getInstance(applicationContext).transactionDao()
        var parsed = 0
        var saved  = 0

        try {
            val cursor = contentResolver.query(
                Uri.parse("content://sms/inbox"),
                arrayOf("_id", "address", "body", "date"),
                null, null, "date DESC"
            ) ?: run { Log.e(TAG, "Cursor null — READ_SMS missing?"); return }

            Log.d(TAG, "Inbox has ${cursor.count} messages")

            cursor.use { c ->
                val idCol      = c.getColumnIndexOrThrow("_id")
                val addressCol = c.getColumnIndexOrThrow("address")
                val bodyCol    = c.getColumnIndexOrThrow("body")
                val dateCol    = c.getColumnIndexOrThrow("date")

                while (c.moveToNext()) {
                    val smsId   = c.getLong(idCol)
                    val address = c.getString(addressCol)
                    val body    = c.getString(bodyCol) ?: continue
                    val date    = c.getLong(dateCol)

                    // Skip SMS from regular phone numbers — only process bank sender IDs
                    if (!isBankSender(address)) {
                        continue
                    }

                    val txn = parseTransaction(body, smsId, date) ?: continue
                    parsed++

                    if (dao.findBySmsMessageId(smsId) == null) {
                        dao.upsert(txn)
                        saved++
                        Log.d(TAG, "Saved: ${txn.merchant} ₹${txn.amount}")
                    }
                }
            }

            Log.d(TAG, "Scan done: parsed=$parsed saved=$saved")
        } catch (e: Exception) {
            Log.e(TAG, "Full scan error: ${e.message}", e)
        }
    }

    private suspend fun parseSingleAndSave(body: String, smsId: Long?, date: Long) {
        try {
            val txn = parseTransaction(body, smsId, date) ?: return
            val dao = FBuddyDatabase.getInstance(applicationContext).transactionDao()
            if (smsId == null || dao.findBySmsMessageId(smsId) == null) {
                dao.upsert(txn)
                Log.d(TAG, "Live SMS saved: ${txn.merchant} ₹${txn.amount}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "parseSingleAndSave error: ${e.message}", e)
        }
    }
}
