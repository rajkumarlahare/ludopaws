package com.rekixo.ludopaws.feature.safety

import android.content.Context

/**
 * Age choice is stored only on this device. It is never added to API requests,
 * account metadata, analytics payloads, or multiplayer messages.
 */
internal enum class AgeRange(
    val label: String,
    val isUnder13: Boolean,
    val isUnder18: Boolean,
) {
    FIVE_AND_UNDER("5 and under", true, true),
    SIX_TO_EIGHT("6–8", true, true),
    NINE_TO_TWELVE("9–12", true, true),
    THIRTEEN_TO_FIFTEEN("13–15", false, true),
    SIXTEEN_TO_SEVENTEEN("16–17", false, true),
    EIGHTEEN_AND_OVER("18 and over", false, false);

    companion object {
        fun fromAge(age: Int): AgeRange? = when {
            age !in 0..120 -> null
            age <= 5 -> FIVE_AND_UNDER
            age <= 8 -> SIX_TO_EIGHT
            age <= 12 -> NINE_TO_TWELVE
            age <= 15 -> THIRTEEN_TO_FIFTEEN
            age <= 17 -> SIXTEEN_TO_SEVENTEEN
            else -> EIGHTEEN_AND_OVER
        }
    }
}

internal object AgeSafetyPolicy {
    private const val PREFS = "ludo_paws_age_safety"
    private const val AGE_RANGE_KEY = "age_range_v1"

    fun selectedAgeRange(context: Context): AgeRange? {
        val value = preferences(context).getString(AGE_RANGE_KEY, null) ?: return null
        return AgeRange.values().firstOrNull { it.name == value }
    }

    fun hasSelectedAgeRange(context: Context): Boolean = selectedAgeRange(context) != null

    fun setSelectedAgeRange(context: Context, range: AgeRange) {
        preferences(context).edit().putString(AGE_RANGE_KEY, range.name).apply()
    }

    fun shouldTreatAsChildForAds(range: AgeRange?): Boolean =
        range == null || range.isUnder13

    fun isUnderAgeOfConsent(range: AgeRange?): Boolean =
        range == null || range.isUnder18

    fun canUseSocialFeaturesForAge(range: AgeRange?): Boolean =
        range != null && !range.isUnder18

    fun canUseOnlineFeaturesForAge(range: AgeRange?): Boolean =
        range != null && !range.isUnder18

    fun canUseOnlineFeatures(context: Context): Boolean =
        canUseOnlineFeaturesForAge(selectedAgeRange(context))

    fun canUseSocialFeatures(context: Context): Boolean =
        canUseSocialFeaturesForAge(selectedAgeRange(context))

    private fun preferences(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
