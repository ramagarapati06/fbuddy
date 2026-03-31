package com.example.fbuddy.data.model

/**
 * High-level spending categories for all transactions.
 *
 * Display names are provided for use in the UI.
 */
enum class Category(val displayName: String) {
    FOOD_DINING("Food & Dining"),
    TRAVEL_TRANSPORT("Travel & Transport"),
    SHOPPING("Shopping"),
    UTILITIES_BILLS("Utilities & Bills"),
    ENTERTAINMENT("Entertainment"),
    HEALTH_MEDICAL("Health & Medical"),
    EDUCATION("Education"),
    GROCERIES("Groceries"),
    FUEL("Fuel"),
    OTHER("Other");
}

