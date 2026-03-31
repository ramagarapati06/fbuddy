package com.example.fbuddy.ui.chat

import android.util.Log
import com.example.fbuddy.data.model.TransactionType
import com.example.fbuddy.data.repository.TransactionRepository
import com.example.fbuddy.utils.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class ChatEngine(
    private val repository: TransactionRepository
) {
    companion object {
        private const val TAG = "ChatEngine"

        // ── PASTE YOUR GEMINI KEY BETWEEN THE QUOTES BELOW ──────────────
        private const val GEMINI_API_KEY = "AIzaSyA6o6kyOJVDFWf8ES-yBg4E39PZhlBMhHU"
        // ────────────────────────────────────────────────────────────────

        private val GEMINI_URL
            get() = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$GEMINI_API_KEY"
    }

    suspend fun answer(message: String): String {
        val trimmed = message.trim()
        if (trimmed.isBlank()) return "Ask me something about your spending!"

        val context = buildFinancialContext()

        if (context == null) {
            return "No transactions found yet. Go to Settings → Re-scan SMS Inbox first!"
        }

        if (GEMINI_API_KEY == "YOUR_GEMINI_API_KEY_HERE") {
            Log.w(TAG, "Gemini API key not set, using local answers")
            return localAnswer(trimmed.lowercase(Locale.getDefault()), context)
        }

        return try {
            val geminiResponse = withContext(Dispatchers.IO) {
                callGemini(trimmed, context)
            }
            geminiResponse ?: localAnswer(trimmed.lowercase(Locale.getDefault()), context)
        } catch (e: Exception) {
            Log.e(TAG, "Gemini failed: ${e.message}", e)
            "Could not reach Gemini. Here's a local answer:\n\n" +
                    localAnswer(trimmed.lowercase(Locale.getDefault()), context)
        }
    }

    private suspend fun buildFinancialContext(): String? {
        return try {
            val now = System.currentTimeMillis()
            val todayStart  = DateUtils.startOfDayMillis(now)
            val monthStart  = DateUtils.startOfMonthMillis(now)
            val last30Start = DateUtils.startOfDaysAgoMillis(30, now)
            val last7Start  = DateUtils.startOfDaysAgoMillis(7, now)

            val todayTxns  = repository.getTransactionsInRange(todayStart, now).first()
            val monthTxns  = repository.getTransactionsInRange(monthStart, now).first()
            val last30Txns = repository.getTransactionsInRange(last30Start, now).first()
            val last7Txns  = repository.getTransactionsInRange(last7Start, now).first()

            if (last30Txns.isEmpty()) return null

            val todayDebit  = todayTxns.filter  { it.type == TransactionType.DEBIT  }.sumOf { it.amount }
            val monthDebit  = monthTxns.filter  { it.type == TransactionType.DEBIT  }.sumOf { it.amount }
            val monthCredit = monthTxns.filter  { it.type == TransactionType.CREDIT }.sumOf { it.amount }
            val last7Debit  = last7Txns.filter  { it.type == TransactionType.DEBIT  }.sumOf { it.amount }
            val dailyAvg    = if (last7Txns.isNotEmpty()) last7Debit / 7 else 0.0

            val categoryBreakdown = monthTxns
                .filter { it.type == TransactionType.DEBIT }
                .groupBy { it.category.displayName }
                .mapValues { (_, list) -> list.sumOf { it.amount } }
                .entries.sortedByDescending { it.value }
                .joinToString("\n") { "  * ${it.key}: Rs.${"%.2f".format(it.value)}" }

            val topMerchants = last30Txns
                .filter { it.type == TransactionType.DEBIT && !it.merchant.isNullOrBlank() }
                .groupBy { it.merchant!!.trim() }
                .mapValues { (_, list) -> list.sumOf { it.amount } }
                .entries.sortedByDescending { it.value }.take(6)
                .joinToString("\n") { "  * ${it.key}: Rs.${"%.2f".format(it.value)}" }

            val recentList = last30Txns.take(8).joinToString("\n") { txn ->
                val sign = if (txn.type == TransactionType.DEBIT) "-" else "+"
                "  ${txn.merchant ?: txn.category.displayName}: ${sign}Rs.${"%.2f".format(txn.amount)} (${txn.category.displayName})"
            }

            """
FINANCIAL SUMMARY:
Today spent: Rs.${"%.2f".format(todayDebit)}
This month spent: Rs.${"%.2f".format(monthDebit)}
This month received: Rs.${"%.2f".format(monthCredit)}
Last 7 days: Rs.${"%.2f".format(last7Debit)} | Daily avg: Rs.${"%.2f".format(dailyAvg)}

CATEGORY BREAKDOWN (this month):
$categoryBreakdown

TOP MERCHANTS (last 30 days):
$topMerchants

RECENT TRANSACTIONS:
$recentList

TOTAL TRANSACTIONS: ${last30Txns.size}
            """.trimIndent()

        } catch (e: Exception) {
            Log.e(TAG, "Error building context: ${e.message}", e)
            null
        }
    }

    private fun callGemini(userMessage: String, financialContext: String): String? {
        val fullPrompt = """
You are F-Buddy, a friendly personal finance assistant for an Indian user.
You have their REAL transaction data below. Give specific, helpful, actionable answers.
Use Rs. for amounts. Keep responses under 200 words. Reference their actual numbers.

$financialContext

User asks: $userMessage
        """.trimIndent()

        Log.d(TAG, "Calling Gemini API with key: ${GEMINI_API_KEY.take(8)}...")

        val requestBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", fullPrompt))
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.8)
                put("maxOutputTokens", 400)
            })
        }

        val url  = URL(GEMINI_URL)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            doOutput       = true
            connectTimeout = 12000
            readTimeout    = 20000
        }

        try {
            conn.outputStream.use { it.write(requestBody.toString().toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            Log.d(TAG, "Gemini HTTP response: $code")

            if (code != 200) {
                val err = conn.errorStream?.bufferedReader()?.readText() ?: "no error body"
                Log.e(TAG, "Gemini error ($code): $err")
                return null
            }

            val responseText = BufferedReader(InputStreamReader(conn.inputStream)).readText()
            val json = JSONObject(responseText)
            return json
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
                .trim()

        } finally {
            conn.disconnect()
        }
    }

    private suspend fun localAnswer(normalized: String, context: String): String {
        val now = System.currentTimeMillis()
        return when {
            normalized.contains("today")                                   -> todaySpend(now)
            normalized.contains("yesterday")                               -> yesterdaySpend(now)
            normalized.contains("week")                                    -> weekSpend(now)
            normalized.contains("month")                                   -> monthSpend(now)
            normalized.contains("top") || normalized.contains("merchant") -> topMerchants(now)
            normalized.contains("categor")                                 -> categoryBreakdown(now)
            normalized.contains("tip") || normalized.contains("save")     -> savingsTip(now)
            else -> "Ask me about: today / this week / this month spending, top merchants, categories, or saving tips!"
        }
    }

    private suspend fun todaySpend(now: Long): String {
        val txns   = repository.getTransactionsInRange(DateUtils.startOfDayMillis(now), now).first()
        val debits = txns.filter { it.type == TransactionType.DEBIT }
        return "You've spent Rs.${"%.2f".format(debits.sumOf { it.amount })} today across ${debits.size} transaction(s)."
    }

    private suspend fun yesterdaySpend(now: Long): String {
        val end   = DateUtils.startOfDayMillis(now) - 1
        val start = DateUtils.startOfDaysAgoMillis(1, now)
        val txns  = repository.getTransactionsInRange(start, end).first()
        return "You spent Rs.${"%.2f".format(txns.filter { it.type == TransactionType.DEBIT }.sumOf { it.amount })} yesterday."
    }

    private suspend fun weekSpend(now: Long): String {
        val txns  = repository.getTransactionsInRange(DateUtils.startOfDaysAgoMillis(7, now), now).first()
        val total = txns.filter { it.type == TransactionType.DEBIT }.sumOf { it.amount }
        return "Last 7 days: Rs.${"%.2f".format(total)} spent. Daily average: Rs.${"%.2f".format(total / 7)}."
    }

    private suspend fun monthSpend(now: Long): String {
        val txns   = repository.getTransactionsInRange(DateUtils.startOfMonthMillis(now), now).first()
        val spent  = txns.filter { it.type == TransactionType.DEBIT  }.sumOf { it.amount }
        val earned = txns.filter { it.type == TransactionType.CREDIT }.sumOf { it.amount }
        return "This month: Rs.${"%.2f".format(spent)} spent, Rs.${"%.2f".format(earned)} received."
    }

    private suspend fun topMerchants(now: Long): String {
        val txns = repository.getTransactionsInRange(DateUtils.startOfDaysAgoMillis(30, now), now).first()
            .filter { it.type == TransactionType.DEBIT && !it.merchant.isNullOrBlank() }
        if (txns.isEmpty()) return "No merchant data in the last 30 days."
        val lines = txns.groupBy { it.merchant!!.trim() }
            .mapValues { (_, l) -> l.sumOf { it.amount } }
            .entries.sortedByDescending { it.value }.take(5)
            .mapIndexed { i, e -> "${i + 1}. ${e.key} — Rs.${"%.2f".format(e.value)}" }
        return "Top merchants (last 30 days):\n${lines.joinToString("\n")}"
    }

    private suspend fun categoryBreakdown(now: Long): String {
        val txns  = repository.getTransactionsInRange(DateUtils.startOfMonthMillis(now), now).first()
            .filter { it.type == TransactionType.DEBIT }
        if (txns.isEmpty()) return "No spending this month yet."
        val total = txns.sumOf { it.amount }
        val lines = txns.groupBy { it.category.displayName }
            .mapValues { (_, l) -> l.sumOf { it.amount } }
            .entries.sortedByDescending { it.value }
            .map { "* ${it.key}: Rs.${"%.2f".format(it.value)} (${"%.0f".format(it.value / total * 100)}%)" }
        return "This month by category:\n${lines.joinToString("\n")}"
    }

    private suspend fun savingsTip(now: Long): String {
        val txns = repository.getTransactionsInRange(DateUtils.startOfMonthMillis(now), now).first()
            .filter { it.type == TransactionType.DEBIT }
        if (txns.isEmpty()) return "Add some transactions first for personalised tips!"
        val top   = txns.groupBy { it.category.displayName }
            .mapValues { (_, l) -> l.sumOf { it.amount } }.maxByOrNull { it.value }
            ?: return "Keep tracking your expenses!"
        val total = txns.sumOf { it.amount }
        val pct   = (top.value / total * 100).toInt()
        return "Your biggest spend is ${top.key} at Rs.${"%.2f".format(top.value)} ($pct% of total). Try setting a monthly limit for this category!"
    }
}
