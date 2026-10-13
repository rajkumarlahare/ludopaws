package com.ludoproof.game.feature.store.data.ads

import android.app.Activity
import android.util.Log
import com.ludoproof.game.feature.account.data.AccountRepository
import com.ludoproof.game.feature.store.data.local.GemWalletStore
import com.ludoproof.game.feature.store.domain.PostWinInterstitialPolicy
import java.util.concurrent.Executors

/**
 * Coordinates the one non-rewarded ad placement against the current server-owned entitlement.
 *
 * The Remove Ads state is freshly read from the authenticated wallet immediately before display.
 * If the entitlement cannot be verified, navigation wins: no interstitial is shown.
 * Rewarded ads are deliberately not consulted here and remain available for their explicit reward.
 */
internal class PostWinInterstitialExitGate(
    private val activity: Activity,
    private val accountRepository: AccountRepository,
    private val walletStore: GemWalletStore,
    private val interstitial: PostWinInterstitialAdController,
) {
    private val executor = Executors.newSingleThreadExecutor { task ->
        Thread(task, "LudoPawsPostWinAdGate").apply { isDaemon = true }
    }

    private var pending = false
    private var closed = false

    /**
     * Returns true when an eligible, loaded interstitial exit check has been accepted.
     * Returns false when callers should navigate immediately (disabled or no ad ready).
     */
    fun interceptIfReady(onExit: () -> Unit): Boolean {
        if (!interstitial.isReady() || activity.isFinishing || activity.isDestroyed) return false
        synchronized(this) {
            if (closed) return false
            if (pending) return true
            pending = true
        }

        try {
            executor.execute {
                val mayShowInterstitial = try {
                    val session = accountRepository.ensureGuestSession()
                    val wallet = accountRepository.wallet()
                    check(wallet.optString("accountId") == session.accountId) {
                        "Wallet account did not match the active account."
                    }
                    check(wallet.has("adsRemoved") && wallet.opt("adsRemoved") is Boolean) {
                        "The server wallet did not include a verified Remove Ads entitlement."
                    }
                    check(walletStore.applyServerSnapshot(wallet, session.accountId)) {
                        "The server wallet snapshot could not be applied to the active account."
                    }
                    PostWinInterstitialPolicy.shouldShowFillerInterstitial(
                        adsRemoved = wallet.getBoolean("adsRemoved"),
                    )
                } catch (error: Exception) {
                    // Fail open to navigation, not to ads: never show a filler ad if ownership
                    // is stale or the server cannot confirm whether Remove Ads was purchased.
                    Log.w(TAG, "Could not verify the Remove Ads entitlement; skipping interstitial", error)
                    false
                }

                activity.runOnUiThread {
                    synchronized(this) { pending = false }
                    if (closed || activity.isFinishing || activity.isDestroyed) return@runOnUiThread

                    if (mayShowInterstitial && interstitial.showIfReady(onExit)) {
                        return@runOnUiThread
                    }
                    onExit()
                }
            }
        } catch (error: Exception) {
            synchronized(this) { pending = false }
            Log.w(TAG, "Could not schedule the post-win entitlement check; skipping interstitial", error)
            return false
        }
        return true
    }

    fun close() {
        synchronized(this) { closed = true }
        executor.shutdownNow()
    }

    private companion object {
        const val TAG = "LudoPawsPostWinAdGate"
    }
}
