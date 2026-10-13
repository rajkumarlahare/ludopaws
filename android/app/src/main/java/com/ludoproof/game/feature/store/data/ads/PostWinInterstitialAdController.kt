package com.ludoproof.game.feature.store.data.ads

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.InterstitialAd
import com.google.android.gms.ads.InterstitialAdLoadCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * One-shot interstitial used only when the player leaves a completed winning result.
 * Missing consent, network failure, or an unloaded ad must never block game navigation.
 */
internal class PostWinInterstitialAdController(
    private val activity: Activity,
    private val enabled: Boolean,
    private val adUnitId: String,
) {
    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(activity)

    private var consentUpdateInProgress = false
    private var sdkInitializationInProgress = false
    private var sdkInitialized = false
    private var adLoading = false
    private var showConsumed = false
    private var showing = false
    private var closed = false
    private var interstitialAd: InterstitialAd? = null

    fun prepare() {
        if (!enabled || closed || interstitialAd != null || adLoading ||
            consentUpdateInProgress || showing
        ) {
            return
        }

        consentUpdateInProgress = true
        consentInformation.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
                    consentUpdateInProgress = false
                    if (error != null) {
                        Log.w(TAG, "Ad consent form could not be shown: ${error.message}")
                    }
                    initializeAdsIfAllowed()
                }
            },
            { error ->
                consentUpdateInProgress = false
                Log.w(TAG, "Ad consent information update failed: ${error.message}")
                initializeAdsIfAllowed()
            },
        )
    }

    /**
     * Returns true only when an already-loaded ad was shown. The caller should finish
     * immediately when false; there is no wait screen or delayed navigation for ad loading.
     */
    fun showIfReady(onFinished: () -> Unit): Boolean {
        if (!enabled || closed || showConsumed || showing) return false
        val ad = interstitialAd ?: return false

        interstitialAd = null
        showConsumed = true
        showing = true
        var completed = false

        fun finishOnce() {
            if (completed) return
            completed = true
            showing = false
            onFinished()
        }

        ad.fullScreenContentCallback =
            object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    finishOnce()
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    Log.w(TAG, "Post-win interstitial failed to show: ${error.message}")
                    finishOnce()
                }
            }

        try {
            ad.show(activity)
        } catch (error: Exception) {
            Log.w(TAG, "Post-win interstitial could not be shown", error)
            finishOnce()
        }
        return true
    }

    fun close() {
        closed = true
        interstitialAd = null
    }

    private fun initializeAdsIfAllowed() {
        if (closed || !consentInformation.canRequestAds()) return
        if (sdkInitialized) {
            loadInterstitial()
            return
        }
        if (sdkInitializationInProgress) return

        sdkInitializationInProgress = true
        MobileAds.initialize(activity) {
            sdkInitializationInProgress = false
            if (closed) return@initialize
            sdkInitialized = true
            loadInterstitial()
        }
    }

    private fun loadInterstitial() {
        if (closed || !consentInformation.canRequestAds() || adLoading ||
            interstitialAd != null || showing
        ) {
            return
        }

        adLoading = true
        InterstitialAd.load(
            activity,
            adUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    adLoading = false
                    if (!closed && consentInformation.canRequestAds()) {
                        interstitialAd = ad
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    adLoading = false
                    Log.w(TAG, "Post-win interstitial failed to load: ${error.message}")
                    interstitialAd = null
                }
            },
        )
    }

    private companion object {
        const val TAG = "LudoPawsPostWinAd"
    }
}
