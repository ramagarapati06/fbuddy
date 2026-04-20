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
        if (trimmed.isBlank()) return "Hey! Ask me anything about your spending - I'm here to help! 😊"

        val context = buildFinancialContext()

        if (context == null) {
            return "Hmm, looks like you haven't added any transactions yet. Head to Settings → Re-scan SMS Inbox to get started! 📱"
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
            "Oops, I'm having trouble connecting right now. But here's what I can tell you:\n\n" +
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
                .joinToString("\n") { "  * ${it.key}: ₹${"%.0f".format(it.value)}" }

            val topMerchants = last30Txns
                .filter { it.type == TransactionType.DEBIT && !it.merchant.isNullOrBlank() }
                .groupBy { it.merchant!!.trim() }
                .mapValues { (_, list) -> list.sumOf { it.amount } }
                .entries.sortedByDescending { it.value }.take(6)
                .joinToString("\n") { "  * ${it.key}: ₹${"%.0f".format(it.value)}" }

            val recentList = last30Txns.take(8).joinToString("\n") { txn ->
                val sign = if (txn.type == TransactionType.DEBIT) "-" else "+"
                "  ${txn.merchant ?: txn.category.displayName}: ${sign}₹${"%.0f".format(txn.amount)} (${txn.category.displayName})"
            }

            """
FINANCIAL DATA:
Today's spending: ₹${"%.0f".format(todayDebit)}
This month spent: ₹${"%.0f".format(monthDebit)}
This month received: ₹${"%.0f".format(monthCredit)}
Last 7 days: ₹${"%.0f".format(last7Debit)} | Daily average: ₹${"%.0f".format(dailyAvg)}

SPENDING BY CATEGORY (this month):
$categoryBreakdown

TOP PLACES THEY SHOP (last 30 days):
$topMerchants

RECENT TRANSACTIONS:
$recentList

Total transactions in last 30 days: ${last30Txns.size}
            """.trimIndent()

        } catch (e: Exception) {
            Log.e(TAG, "Error building context: ${e.message}", e)
            null
        }
    }

    private fun callGemini(userMessage: String, financialContext: String): String? {
        val fullPrompt = """
You are F-Buddy, a friendly and helpful personal finance buddy for an Indian user. Think of yourself as their financially savvy friend who looks at their actual spending data and gives practical, caring advice.

PERSONALITY GUIDELINES:
- Be warm, conversational, and encouraging - like texting a friend
- Use casual language and emojis occasionally (but don't overdo it)
- Celebrate wins ("Nice! You're doing great!") and be supportive about challenges
- Give SPECIFIC numbers from their actual data - never be vague
- Keep responses natural and under 150 words
- Use "₹" for rupees, not "Rs."
- When giving advice, make it actionable and relevant to their actual spending
- Avoid corporate jargon - talk like a real person
- If they're overspending, be honest but kind, not preachy

THEIR ACTUAL FINANCIAL DATA:
$financialContext

USER'S QUESTION: $userMessage

Remember: Reference their REAL numbers and patterns. Make them feel like you actually looked at their data, because you did!
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
                put("temperature", 0.9)  // Increased for more natural variety
                put("maxOutputTokens", 300)
                put("topP", 0.95)
                put("topK", 40)
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
            else -> "I can help you with:\n\n💰 Today's spending\n📊 This week/month's stats\n🏪 Top merchants\n📈 Spending by category\n💡 Savings tips\n\nWhat would you like to know?"
        }
    }

    private suspend fun todaySpend(now: Long): String {
        val txns   = repository.getTransactionsInRange(DateUtils.startOfDayMillis(now), now).first()
        val debits = txns.filter { it.type == TransactionType.DEBIT }
        val total = debits.sumOf { it.amount }

        return when {
            debits.isEmpty() -> "You haven't spent anything today yet! 🎉 Starting the day strong!"
            total < 200 -> "You've spent ₹${"%.0f".format(total)} today across ${debits.size} transaction(s). Pretty light! 😊"
            total < 1000 -> "Today's spending is ₹${"%.0f".format(total)} across ${debits.size} transaction(s). Looking good!"
            else -> "You've spent ₹${"%.0f".format(total)} today (${debits.size} transactions). That's a bit high - maybe ease up a bit? 😅"
        }
    }

    private suspend fun yesterdaySpend(now: Long): String {
        val end   = DateUtils.startOfDayMillis(now) - 1
        val start = DateUtils.startOfDaysAgoMillis(1, now)
        val txns  = repository.getTransactionsInRange(start, end).first()
        val total = txns.filter { it.type == TransactionType.DEBIT }.sumOf { it.amount }

        return if (total == 0.0) {
            "You didn't spend anything yesterday! Either that's impressive restraint or you forgot to scan your messages 😄"
        } else {
            "Yesterday you spent ₹${"%.0f".format(total)}. ${if (total > 1000) "That was a busy day!" else "Not bad!"}"
        }
    }

    private suspend fun weekSpend(now: Long): String {
        val txns  = repository.getTransactionsInRange(DateUtils.startOfDaysAgoMillis(7, now), now).first()
        val total = txns.filter { it.type == TransactionType.DEBIT }.sumOf { it.amount }
        val daily = total / 7

        return "In the last 7 days, you've spent ₹${"%.0f".format(total)}.\n\nThat's about ₹${"%.0f".format(daily)}/day on average. ${
            when {
                daily < 300 -> "Super disciplined! 💪"
                daily < 800 -> "Pretty reasonable pace!"
                else -> "Running a bit hot - maybe dial it back? 🔥"
            }
        }"
    }

    private suspend fun monthSpend(now: Long): String {
        val txns   = repository.getTransactionsInRange(DateUtils.startOfMonthMillis(now), now).first()
        val spent  = txns.filter { it.type == TransactionType.DEBIT  }.sumOf { it.amount }
        val earned = txns.filter { it.type == TransactionType.CREDIT }.sumOf { it.amount }

        return "This month so far:\n💸 Spent: ₹${"%.0f".format(spent)}\n💰 Received: ₹${"%.0f".format(earned)}\n\n${
            if (earned > spent) "You're in the green! Nice work! 🎉"
            else if (spent > earned * 0.8) "Getting close to what you earned - watch out!"
            else "You're doing alright, keep it up!"
        }"
    }

    private suspend fun topMerchants(now: Long): String {
        val txns = repository.getTransactionsInRange(DateUtils.startOfDaysAgoMillis(30, now), now).first()
            .filter { it.type == TransactionType.DEBIT && !it.merchant.isNullOrBlank() }
        if (txns.isEmpty()) return "No merchant data yet. Once you scan some SMS, I'll show you where your money's going! 📊"

        val lines = txns.groupBy { it.merchant!!.trim() }
            .mapValues { (_, l) -> l.sumOf { it.amount } }
            .entries.sortedByDescending { it.value }.take(5)
            .mapIndexed { i, e -> "${i + 1}. ${e.key} — ₹${"%.0f".format(e.value)}" }

        return "Your top 5 hangout spots (last 30 days):\n\n${lines.joinToString("\n")}\n\n${
            if (lines.size >= 3) "Looks like you have some favorites! 😄" else ""
        }"
    }

    private suspend fun categoryBreakdown(now: Long): String {
        val txns  = repository.getTransactionsInRange(DateUtils.startOfMonthMillis(now), now).first()
            .filter { it.type == TransactionType.DEBIT }
        if (txns.isEmpty()) return "You haven't spent anything this month yet! Fresh start! 🌟"

        val total = txns.sumOf { it.amount }
        val lines = txns.groupBy { it.category.displayName }
            .mapValues { (_, l) -> l.sumOf { it.amount } }
            .entries.sortedByDescending { it.value }
            .take(5)
            .map { "${it.key}: ₹${"%.0f".format(it.value)} (${"%.0f".format(it.value / total * 100)}%)" }

        val topCategory = lines.firstOrNull()?.substringBefore(":")
        return "Here's where your money went this month:\n\n${lines.joinToString("\n")}\n\n${
            topCategory?.let { "Wow, $it is taking the lead! 🏆" } ?: ""
        }"
    }

    private suspend fun savingsTip(now: Long): String {
        val txns = repository.getTransactionsInRange(DateUtils.startOfMonthMillis(now), now).first()
            .filter { it.type == TransactionType.DEBIT }
        if (txns.isEmpty()) return "Start tracking your spending, and I'll give you personalized tips! 💡"

        val top   = txns.groupBy { it.category.displayName }
            .mapValues { (_, l) -> l.sumOf { it.amount } }.maxByOrNull { it.value }
            ?: return "Keep tracking - you're building good habits! 💪"

        val total = txns.sumOf { it.amount }
        val pct   = (top.value / total * 100).toInt()

        return "💡 Your biggest expense is ${top.key} at ₹${"%.0f".format(top.value)} (that's $pct% of your spending!).\n\n" +
                "Try this: Set a monthly cap for ${top.key}. Even cutting it by 20% would save you ₹${"%.0f".format(top.value * 0.2)}! 🎯"
    }
}