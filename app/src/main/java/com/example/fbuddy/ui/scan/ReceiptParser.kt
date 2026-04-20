package com.example.fbuddy.ui.scan

import java.util.Locale

data class ParsedReceipt(
    val merchant: String? = null,
    val totalAmount: Double? = null,
    val category: com.example.fbuddy.data.model.Category =
        com.example.fbuddy.data.model.Category.OTHER,
    val rawText: String
)

/**
 * Improved receipt parser:
 *  - Skips address/phone/date lines to find a real merchant name
 *  - Prefers explicit TOTAL lines; falls back to largest plausible amount
 *  - Returns category so it can be edited before saving
 */
object ReceiptParser {

    // Matches ₹, Rs, INR followed by a number, OR a plain number on a TOTAL line
    private val amountRegex = Regex(
        """(?:₹|rs\.?|inr)?\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    // Patterns that indicate a line is NOT a merchant name
    private val skipPatterns = listOf(
        Regex("""^\+?[0-9\-\s]{7,}$"""),                  // phone numbers
        Regex("""[0-9]{1,2}[/-][0-9]{1,2}[/-][0-9]{2,4}"""), // dates
        Regex("""^\d+$"""),                                 // pure numbers
        Regex("""(gstin|gst no|pan|cin|tax|invoice|bill no|receipt|www\.|http|@)""",
            RegexOption.IGNORE_CASE),
        Regex("""^(thank|welcome|visit|total|amount|cash|card|upi|net|debit|credit|change|balance)""",
            RegexOption.IGNORE_CASE)
    )

    fun parse(rawText: String): ParsedReceipt {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }

        val merchant = findMerchant(lines)
        val total    = findTotal(lines) ?: findLargestAmount(rawText)
        val category = com.example.fbuddy.utils.Categorizer.categorize(merchant, rawText)

        return ParsedReceipt(
            merchant    = merchant,
            totalAmount = total,
            category    = category,
            rawText     = rawText
        )
    }

    /** Walk through top lines; skip noise lines; pick first plausible merchant name */
    private fun findMerchant(lines: List<String>): String? {
        // Check first 8 lines — merchant header is almost always near the top
        return lines.take(8).firstOrNull { line ->
            line.length in 3..50 && skipPatterns.none { it.containsMatchIn(line) }
        }
    }

    /** Find a line explicitly labelled as the total */
    private fun findTotal(lines: List<String>): Double? {
        val keywords = listOf("grand total", "net total", "total amount", "amount due",
            "net payable", "total", "payable", "net pay")
        for (keyword in keywords) {
            val line = lines.firstOrNull {
                it.lowercase(Locale.getDefault()).contains(keyword)
            } ?: continue
            val amount = extractFirstAmount(line)
            if (amount != null && amount > 0.0) return amount
        }
        return null
    }

    private fun extractFirstAmount(text: String): Double? {
        val match = amountRegex.find(text) ?: return null
        return match.groupValues.getOrNull(1)?.replace(",", "")?.toDoubleOrNull()
    }

    private fun findLargestAmount(text: String): Double? =
        amountRegex.findAll(text)
            .mapNotNull { it.groupValues.getOrNull(1)?.replace(",", "")?.toDoubleOrNull() }
            .filter { it > 0.0 }
            .maxOrNull()
}
