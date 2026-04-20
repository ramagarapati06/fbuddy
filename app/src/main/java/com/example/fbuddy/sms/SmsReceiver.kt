package com.example.fbuddy.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.fbuddy.data.db.FBuddyDatabase
import com.example.fbuddy.data.db.TransactionEntity
import com.example.fbuddy.data.model.TransactionSource
import com.example.fbuddy.data.model.TransactionType
import com.example.fbuddy.data.repository.TransactionRepository
import com.example.fbuddy.notifications.TransactionNotificationManager
import com.example.fbuddy.utils.Categorizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        try {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (messages.isNullOrEmpty()) return

            // Group multi-part SMS together by sender
            val grouped = messages.groupBy { it.originatingAddress ?: "unknown" }

            for ((_, parts) in grouped) {
                val body = parts.joinToString("") { it.messageBody ?: "" }
                val date = parts.first().timestampMillis

                Log.d(TAG, "SMS received: ${body.take(80)}")

                // Use SmsParser for consistent parsing
                val parsed = SmsParser.parse(body)
                if (parsed != null) {
                    Log.d(TAG, "Payment SMS detected — saving...")
                    scope.launch {
                        saveAndNotify(context, body, parsed, date)
                    }
                } else if (SmsParserService.isBankSms(body)) {
                    // Fallback to SmsParserService for complex patterns
                    Log.d(TAG, "Bank SMS (complex) — sending to SmsParserService")
                    SmsParserService.parseSingleSms(
                        context = context,
                        body    = body,
                        smsId   = date,
                        date    = date
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "SmsReceiver error: ${e.message}", e)
        }
    }

    private suspend fun saveAndNotify(
        context: Context,
        rawBody: String,
        parsed: ParsedSms,
        date: Long
    ) {
        try {
            val db       = FBuddyDatabase.getInstance(context)
            val repo     = TransactionRepository(db)
            val category = Categorizer.categorize(parsed.merchant, rawBody)

            val entity = TransactionEntity(
                amount    = parsed.amount,
                type      = if (parsed.isDebit) TransactionType.DEBIT else TransactionType.CREDIT,
                merchant  = parsed.merchant,
                category  = category,
                timestamp = date,
                source    = TransactionSource.SMS,
                rawText   = rawBody
            )
            repo.upsert(entity)
            Log.d(TAG, "Saved: ${parsed.merchant} ₹${parsed.amount}")

            // Show heads-up notification
            TransactionNotificationManager(context).showTransactionNotification(
                merchant          = parsed.merchant ?: "Unknown Merchant",
                amount            = parsed.amount,
                category          = category,
                source            = "SMS",
                isUnknownMerchant = parsed.merchant == null || parsed.merchant.isBlank()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save SMS transaction: ${e.message}", e)
        }
    }
}
