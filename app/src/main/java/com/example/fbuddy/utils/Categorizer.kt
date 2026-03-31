package com.example.fbuddy.utils

import com.example.fbuddy.data.model.Category

/**
 * Keyword and merchant-name based categorization.
 *
 * This is intentionally simple for v1 and can be evolved to a more
 * sophisticated rules engine later.
 */
object Categorizer {

    private val categoryKeywords: Map<Category, List<String>> = mapOf(
        Category.FOOD_DINING to listOf(
            "zomato", "swiggy", "mcdonald", "kfc", "pizza hut", "dominos", "domino's",
            "faasos", "ubereats", "restaurant", "cafe", "coffee"
        ),
        Category.TRAVEL_TRANSPORT to listOf(
            "ola", "uber", "rapido", "irctc", "indian railways", "makemytrip",
            "redbus", "goibibo", "yatra", "airlines", "metro", "bus", "cab", "flight"
        ),
        Category.SHOPPING to listOf(
            "amazon", "flipkart", "ajio", "myntra", "tata cliq", "snapdeal",
            "nykaa", "shopping", "lifestyle", "shoppers stop"
        ),
        Category.UTILITIES_BILLS to listOf(
            "electricity", "power", "water bill", "gas bill", "dth", "broadband",
            "internet", "postpaid", "prepaid", "mobile bill"
        ),
        Category.ENTERTAINMENT to listOf(
            "netflix", "prime video", "hotstar", "zee5", "sonyliv",
            "bookmyshow", "bms", "cinema", "movie", "ticket"
        ),
        Category.HEALTH_MEDICAL to listOf(
            "pharmacy", "medical", "apollo", "1mg", "practo", "lab", "diagnostic",
            "hospital", "clinic"
        ),
        Category.EDUCATION to listOf(
            "byjus", "unacademy", "coursera", "udemy", "edx", "school", "college",
            "university", "coaching", "tuition", "exam"
        ),
        Category.GROCERIES to listOf(
            "bigbasket", "bbdaily", "blinkit", "grofers", "zepto", "jiomart",
            "dmart", "more supermarket", "reliance fresh", "grocery", "supermarket"
        ),
        Category.FUEL to listOf(
            "hpcl", "hindustan petroleum", "iocl", "indian oil", "bpcl",
            "bharat petroleum", "petrol", "diesel", "fuel", "pump"
        )
    )

    fun categorize(merchant: String?, rawText: String?): Category {
        val combined = buildString {
            merchant?.let { append(it).append(' ') }
            rawText?.let { append(it) }
        }.lowercase()

        if (combined.isBlank()) {
            return Category.OTHER
        }

        for ((category, keywords) in categoryKeywords) {
            if (keywords.any { keyword -> combined.contains(keyword) }) {
                return category
            }
        }

        return Category.OTHER
    }
}

