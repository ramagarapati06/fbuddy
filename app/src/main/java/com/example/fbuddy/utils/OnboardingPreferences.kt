package com.example.fbuddy.utils

import android.content.Context

object OnboardingPreferences {

    private const val PREFS_NAME = "fbuddy_prefs"
    private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"

    fun isOnboardingComplete(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ONBOARDING_COMPLETE, false)
    }

    fun setOnboardingComplete(context: Context, complete: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ONBOARDING_COMPLETE, complete)
            .apply()
    }
}

