package com.rekixo.ludopaws.feature.safety

import com.google.android.gms.ads.AgeRestrictedTreatment
import com.rekixo.ludopaws.feature.store.data.ads.FamilyAdRequestPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgeSafetyPolicyTest {
    @Test
    fun missingAgeDefaultsToChildTreatmentAndNoSocialAccess() {
        assertTrue(AgeSafetyPolicy.shouldTreatAsChildForAds(null))
        assertTrue(AgeSafetyPolicy.isUnderAgeOfConsent(null))
        assertFalse(AgeSafetyPolicy.canUseSocialFeaturesForAge(null))
        assertEquals(AgeRestrictedTreatment.CHILD, FamilyAdRequestPolicy.treatmentFor(null))
    }

    @Test
    fun childrenReceiveChildTreatmentAndSocialFeaturesRemainDisabled() {
        AgeRange.values().filter { it.isUnder13 }.forEach { range ->
            assertTrue(AgeSafetyPolicy.shouldTreatAsChildForAds(range))
            assertTrue(range.isUnder18)
            assertFalse(AgeSafetyPolicy.canUseSocialFeaturesForAge(range))
            assertEquals(AgeRestrictedTreatment.CHILD, FamilyAdRequestPolicy.treatmentFor(range))
        }
    }

    @Test
    fun teensReceiveTeenTreatmentButSocialFeaturesRemainDisabled() {
        AgeRange.values().filter { !it.isUnder13 && it.isUnder18 }.forEach { range ->
            assertFalse(AgeSafetyPolicy.shouldTreatAsChildForAds(range))
            assertTrue(AgeSafetyPolicy.isUnderAgeOfConsent(range))
            assertFalse(AgeSafetyPolicy.canUseSocialFeaturesForAge(range))
            assertEquals(AgeRestrictedTreatment.TEEN, FamilyAdRequestPolicy.treatmentFor(range))
        }
    }

    @Test
    fun adultsReceiveUnspecifiedTreatmentAndCanUseSocialFeatures() {
        val adults = AgeRange.values().filter { !it.isUnder18 }
        assertEquals(1, adults.size)
        adults.forEach { range ->
            assertFalse(AgeSafetyPolicy.shouldTreatAsChildForAds(range))
            assertFalse(AgeSafetyPolicy.isUnderAgeOfConsent(range))
            assertTrue(AgeSafetyPolicy.canUseSocialFeaturesForAge(range))
            assertEquals(AgeRestrictedTreatment.UNSPECIFIED, FamilyAdRequestPolicy.treatmentFor(range))
        }
    }

    @Test
    fun ageRangesAreNeutralAndCoverAllSixPlayConsoleBands() {
        assertEquals(6, AgeRange.values().size)
        assertEquals(
            listOf("5 and under", "6–8", "9–12", "13–15", "16–17", "18 and over"),
            AgeRange.values().map { it.label },
        )
    }
}
