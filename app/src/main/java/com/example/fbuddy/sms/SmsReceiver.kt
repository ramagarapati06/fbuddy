package com.example.fbuddy.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        try {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (messages.isNullOrEmpty()) return

            // Group multi-part SMS together
            val grouped = messages.groupBy { it.originatingAddress ?: "unknown" }

            for ((_, parts) in grouped) {
                val body = parts.joinToString("") { it.messageBody ?: "" }
                val date = parts.first().timestampMillis

                Log.d(TAG, "SMS received: ${body.take(80)}")

                if (SmsParserService.isBankSms(body)) {
                    Log.d(TAG, "Bank SMS — sending to parser")
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
}
