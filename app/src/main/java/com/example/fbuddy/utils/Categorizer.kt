package com.example.fbuddy.utils

import com.example.fbuddy.data.model.Category

object Categorizer {

    private val categoryKeywords: Map<Category, List<String>> = mapOf(

        Category.FOOD_DINING to listOf(
            "zomato", "swiggy", "mcdonald", "kfc", "pizza hut", "dominos", "domino's",
            "burger king", "subway", "faasos", "box8", "freshmenu", "eat.fit",
            "starbucks", "cafe coffee day", "barista", "chaayos",
            "restaurant", "cafe", "diner", "bistro", "dhaba", "hotel",
            "food", "kitchen", "canteen", "mess", "tiffin",
        ),

        Category.TRAVEL_TRANSPORT to listOf(
            "ola", "uber", "rapido", "indrive",
            "irctc", "indian railways", "railway", "railyatri",
            "makemytrip", "goibibo", "yatra", "cleartrip", "ixigo",
            "redbus", "abhibus",
            "indigo", "air india", "spicejet", "vistara", "akasa",
            "metro", "dmrc", "bmtc", "ksrtc", "tsrtc", "best bus",
            "cab", "taxi", "auto", "rickshaw", "flight", "bus", "train",
            "toll", "fastag", "parking",
        ),

        Category.SHOPPING to listOf(
            "amazon", "flipkart", "ajio", "myntra", "tata cliq",
            "snapdeal", "nykaa", "meesho", "shopsy", "jiomart",
            "reliance digital", "croma", "vijay sales",
            "lifestyle", "shoppers stop", "pantaloons", "westside",
            "max fashion", "zara", "h&m", "levi", "puma", "nike", "adidas",
            "shopping", "mall", "store", "boutique",
        ),

        Category.GROCERIES to listOf(
            "bigbasket", "blinkit", "zepto", "swiggy instamart",
            "dmart", "d mart", "more supermarket", "reliance fresh",
            "nature's basket", "spencer", "star bazaar",
            "grofers", "milkbasket", "daily basket",
            "grocery", "supermarket", "kirana", "vegetables", "fruits",
            "dairy", "milk", "bread",
        ),

        Category.UTILITIES_BILLS to listOf(
            "electricity", "electric", "power", "bescom", "tsspdcl", "msedcl",
            "water bill", "water supply", "bwssb",
            "gas bill", "lpg", "indane", "hp gas", "bharat gas",
            "broadband", "internet", "fiber", "jio fiber", "airtel fiber",
            "postpaid", "mobile bill", "phone bill",
            "dth", "tata sky", "dish tv", "sun direct",
            "property tax", "municipal",
        ),

        Category.ENTERTAINMENT to listOf(
            "netflix", "amazon prime", "hotstar", "disney+", "zee5",
            "sonyliv", "voot", "altbalaji", "mxplayer",
            "spotify", "apple music", "gaana", "wynk",
            "bookmyshow", "paytm movies", "cinema", "multiplex",
            "pvr", "inox", "cinepolis",
            "game", "gaming", "steam", "playstation", "xbox",
        ),

        Category.HEALTH_MEDICAL to listOf(
            "pharmacy", "medical store", "chemist",
            "apollo pharmacy", "medplus", "wellness forever",
            "1mg", "pharmeasy", "netmeds",
            "hospital", "clinic", "doctor", "dr.",
            "diagnostic", "lab", "pathlab", "thyrocare",
            "practo", "lybrate",
            "gym", "fitness", "cure.fit", "cult.fit",
        ),

        Category.EDUCATION to listOf(
            "byju", "unacademy", "vedantu", "toppr",
            "coursera", "udemy", "linkedin learning",
            "school", "college", "university", "institute",
            "coaching", "tuition", "classes",
            "exam fee", "admission fee",
        ),

        Category.FUEL to listOf(
            "hpcl", "hindustan petroleum", "iocl", "indian oil",
            "bpcl", "bharat petroleum",
            "petrol", "diesel", "fuel", "pump", "cng", "ev charge",
        ),
    )

    fun categorize(merchant: String?, rawText: String?): Category {
        val combined = buildString {
            merchant?.let { append(it.lowercase()).append(' ') }
            rawText?.let   { append(it.lowercase()) }
        }

        if (combined.isBlank()) return Category.OTHER

        for ((category, keywords) in categoryKeywords) {
            if (keywords.any { keyword -> combined.contains(keyword) }) {
                return category
            }
        }

        return Category.OTHER
    }
}
