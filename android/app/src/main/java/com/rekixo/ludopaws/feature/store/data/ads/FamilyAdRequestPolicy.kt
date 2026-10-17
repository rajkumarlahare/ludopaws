package com.rekixo.ludopaws.feature.store.data.ads

import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AgeRestrictedTreatment
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.rekixo.ludopaws.feature.safety.AgeSafetyPolicy
import com.rekixo.ludopaws.feature.safety.AgeRange

/**
 * Applies the selected local age band to every Google Mobile Ads request.
 * Missing age is conservatively treated as CHILD and every request is limited to G content.
 */
internal object FamilyAdRequestPolicy {
    internal fun treatmentFor(range: AgeRange?): AgeRestrictedTreatment =
        when {
            range == null -> AgeRestrictedTreatment.CHILD
            range.isUnder13 -> AgeRestrictedTreatment.CHILD
            range.isUnder18 -> AgeRestrictedTreatment.TEEN
            else -> AgeRestrictedTreatment.UNSPECIFIED
        }

    fun configure(context: Context) {
        val range = AgeSafetyPolicy.selectedAgeRange(context)
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setAgeRestrictedTreatment(treatmentFor(range))
                .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)
                .build(),
        )
    }

    fun createAdRequest(context: Context): AdRequest {
        configure(context)
        return AdRequest.Builder().build()
    }
}
