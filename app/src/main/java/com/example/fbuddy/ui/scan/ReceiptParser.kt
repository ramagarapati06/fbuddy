package com.example.fbuddy.ui.scan

import java.util.Locale

data class ParsedReceipt(
    val merchant: String? = null,
    val totalAmount: Double? = null,
    val rawText: String
)

/**
 * Minimal receipt parser: tries to find a "TOTAL" line and extract the amount.
 * If not found, falls back to the largest amount-like number in the text.
 */
object ReceiptParser {

    private val amountRegex = Regex("""(?:₹|rs\.?|inr)?\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    fun parse(rawText: String): ParsedReceipt {
        val lines = rawText
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val merchant = lines.firstOrNull { it.length in 3..48 }

        val total = findTotal(lines) ?: findLargestAmount(rawText)

        return ParsedReceipt(
            merchant = merchant,
            totalAmount = total,
            rawText = rawText
        )
    }

    private fun findTotal(lines: List<String>): Double? {
        val totalLine = lines.firstOrNull { line ->
            val l = line.lowercase(Locale.getDefault())
            l.contains("total") || l.contains("grand total") || l.contains("amount due")
        } ?: return null

        return extractFirstAmount(totalLine)
    }

    private fun extractFirstAmount(text: String): Double? {
        val match = amountRegex.find(text) ?: return null
        val numeric = match.groupValues.getOrNull(1)?.replace(",", "") ?: return null
        return numeric.toDoubleOrNull()
    }

    private fun findLargestAmount(text: String): Double? {
        val amounts = amountRegex.findAll(text)
            .mapNotNull { it.groupValues.getOrNull(1)?.replace(",", "")?.toDoubleOrNull() }
            .toList()
        return amounts.maxOrNull()
    }
}

