package com.example.fbuddy.sms

import java.util.Locale

data class ParsedSms(
    val amount: Double,
    val merchant: String?,
    val isDebit: Boolean
)

/**
 * Standalone SMS parser — used by both SmsReceiver (live) and SmsParserService (full scan).
 * Keeps one source of truth for all parsing logic.
 */
object SmsParser {

    private val amountRegex = Regex(
        """(?:rs\.?|inr|₹)\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    private val merchantRegex = Regex(
        """(?:at|to|towards|for)\s+([A-Za-z0-9][A-Za-z0-9 &'\-\.]{1,30})(?:\s+on\s|\s+ref|\s+txn|\s*[,.]|$)""",
        RegexOption.IGNORE_CASE
    )

    private val debitKeywords   = listOf("debited", "debit", "paid", "payment", "spent", "transferred", "withdrawn", "deducted")
    private val creditKeywords  = listOf("credited", "credit", "received", "refund", "deposited")
    private val paymentKeywords = listOf("debited", "credited", "transaction", "payment", "upi", "neft", "imps", "rs.", "inr", "₹")

    fun parse(body: String): ParsedSms? {
        val lower = body.lowercase(Locale.getDefault())
        if (paymentKeywords.none { lower.contains(it) }) return null

        val amount = amountRegex.find(body)
            ?.groupValues?.getOrNull(1)
            ?.replace(",", "")
            ?.toDoubleOrNull()
            ?: return null

        if (amount <= 0.0) return null

        val isDebit  = debitKeywords.any { lower.contains(it) } ||
                       !creditKeywords.any { lower.contains(it) }
        val merchant = merchantRegex.find(body)
            ?.groupValues?.getOrNull(1)
            ?.trim()
            ?.takeIf { it.isNotBlank() }

        return ParsedSms(amount = amount, merchant = merchant, isDebit = isDebit)
    }
}
