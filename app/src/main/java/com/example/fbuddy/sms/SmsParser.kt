package com.example.fbuddy.sms

import android.telephony.SmsMessage
import com.example.fbuddy.data.db.TransactionEntity
import com.example.fbuddy.data.model.Category
import com.example.fbuddy.data.model.TransactionSource
import com.example.fbuddy.data.model.TransactionType
import com.example.fbuddy.utils.Categorizer
import java.util.Locale
import java.util.regex.Pattern

/**
 * Parses raw SMS messages into structured transaction records.
 *
 * This class is regex-heavy but intentionally self-contained so it can be
 * iterated on independently of the rest of the app.
 */
class SmsParser {

    data class SmsContext(
        val address: String?,
        val body: String,
        val timestampMillis: Long,
        val smsMessageId: Long? = null
    )

    private val amountPatterns = listOf(
        // "debited by Rs. 1,234.56" / "debited by INR 1234"
        Pattern.compile(
            "(?i)(?:debited(?: by)?|spent)\\s*(?:rs\\.?|inr)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"
        ),
        // "credited with Rs. 1,234.56"
        Pattern.compile(
            "(?i)(?:credited(?: with)?|received)\\s*(?:rs\\.?|inr)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"
        ),
        // "INR 1,234.56 debited from a/c"
        Pattern.compile(
            "(?i)(?:rs\\.?|inr)\\s*([0-9,]+(?:\\.[0-9]{1,2})?).{0,40}?(debited|credited|spent)"
        )
    )

    private val debitKeywords = listOf("debited", "spent", "purchase", "paid", "withdrawn")
    private val creditKeywords = listOf("credited", "received", "deposit", "refunded", "refund")

    private val merchantPatterns = listOf(
        // "spent Rs. X at MERCHANT"
        Pattern.compile("(?i)\\bat\\s+([A-Za-z0-9 &_.'-]{3,})"),
        // "to MERCHANT"
        Pattern.compile("(?i)\\bto\\s+([A-Za-z0-9 &_.'-]{3,})")
    )

    private val accountLast4Pattern = Pattern.compile(
        "(?i)(?:a/c|account)\\s*(?:xx|no\\.?|number)?\\s*[*Xx]*([0-9]{4})"
    )

    private val referencePattern = Pattern.compile(
        "(?i)\\b(?:ref(?:erence)?|txn|transaction)\\s*(?:no\\.?|id)?[:\\s]*([A-Za-z0-9-]{4,})"
    )

    private val bankSenderMap: Map<String, String> = mapOf(
        "SBI" to "State Bank of India",
        "SBINB" to "State Bank of India",
        "HDFC" to "HDFC Bank",
        "HDFCBK" to "HDFC Bank",
        "ICICI" to "ICICI Bank",
        "ICICIB" to "ICICI Bank",
        "AXIS" to "Axis Bank",
        "AXISBK" to "Axis Bank",
        "KOTAK" to "Kotak Mahindra Bank",
        "KOTAKB" to "Kotak Mahindra Bank"
    )

    /**
     * Parse a set of SMS PDUs (from BroadcastReceiver) into a single context.
     */
    fun fromMessages(messages: Array<SmsMessage>): SmsContext? {
        if (messages.isEmpty()) return null

        val body = buildString {
            messages.forEach { append(it.messageBody ?: "") }
        }.trim()

        if (body.isEmpty()) return null

        val address = messages.first().originatingAddress
        val timestamp = messages.first().timestampMillis

        return SmsContext(
            address = address,
            body = body,
            timestampMillis = timestamp
        )
    }

    /**
     * Parse an inbox row into a transaction entity.
     */
    fun parseInboxRow(
        address: String?,
        body: String,
        timestampMillis: Long,
        smsId: Long
    ): TransactionEntity? {
        val ctx = SmsContext(address, body, timestampMillis, smsId)
        return parse(ctx)
    }

    /**
     * Core parsing logic.
     */
    fun parse(context: SmsContext): TransactionEntity? {
        val body = context.body
        val normalizedBody = body.lowercase(Locale.getDefault())

        val amount = extractAmount(body) ?: return null
        val type = inferType(normalizedBody) ?: return null
        val merchant = extractMerchant(body)
        val bankName = inferBankName(context.address, normalizedBody)
        val accountLast4 = extractAccountLast4(body)
        val referenceId = extractReferenceId(body)
        val category = Categorizer.categorize(merchant, body)

        return TransactionEntity(
            amount = amount,
            type = type,
            merchant = merchant,
            category = category,
            timestamp = context.timestampMillis,
            source = TransactionSource.SMS,
            rawText = body,
            bankName = bankName,
            accountLast4 = accountLast4,
            currencyCode = "INR",
            referenceId = referenceId,
            smsMessageId = context.smsMessageId
        )
    }

    private fun extractAmount(body: String): Double? {
        for (pattern in amountPatterns) {
            val matcher = pattern.matcher(body)
            if (matcher.find()) {
                val raw = matcher.group(1) ?: continue
                val numeric = raw.replace(",", "")
                return numeric.toDoubleOrNull()
            }
        }
        return null
    }

    private fun inferType(normalizedBody: String): TransactionType? {
        if (debitKeywords.any { normalizedBody.contains(it) }) {
            return TransactionType.DEBIT
        }
        if (creditKeywords.any { normalizedBody.contains(it) }) {
            return TransactionType.CREDIT
        }
        return null
    }

    private fun extractMerchant(body: String): String? {
        for (pattern in merchantPatterns) {
            val matcher = pattern.matcher(body)
            if (matcher.find()) {
                val candidate = matcher.group(1)?.trim()
                if (!candidate.isNullOrBlank()) {
                    // Avoid obviously non-merchant generic words
                    val cleaned = candidate.trim { it <= ' ' }
                    if (cleaned.length >= 3) {
                        return cleaned
                    }
                }
            }
        }
        return null
    }

    private fun extractAccountLast4(body: String): String? {
        val matcher = accountLast4Pattern.matcher(body)
        return if (matcher.find()) matcher.group(1) else null
    }

    private fun extractReferenceId(body: String): String? {
        val matcher = referencePattern.matcher(body)
        return if (matcher.find()) matcher.group(1) else null
    }

    private fun inferBankName(address: String?, normalizedBody: String): String? {
        if (address.isNullOrBlank()) return null

        val upperSender = address.uppercase(Locale.getDefault())

        bankSenderMap.forEach { (key, value) ->
            if (upperSender.contains(key)) {
                return value
            }
        }

        // Fallback: try simple contains in body
        return when {
            normalizedBody.contains("sbi") -> "State Bank of India"
            normalizedBody.contains("hdfc") -> "HDFC Bank"
            normalizedBody.contains("icici") -> "ICICI Bank"
            normalizedBody.contains("axis bank") -> "Axis Bank"
            normalizedBody.contains("kotak") -> "Kotak Mahindra Bank"
            else -> null
        }
    }
}

