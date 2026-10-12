package com.ludoproof.game.feature.profile.presentation

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialManagerCallback
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.ludoproof.game.LudoProofTheme
import com.ludoproof.game.LudoProofApplication
import com.ludoproof.game.feature.account.data.AccountApiException
import com.ludoproof.game.feature.friends.data.local.FriendCredential
import com.ludoproof.game.feature.friends.data.local.FriendIdentityStore
import com.ludoproof.game.feature.leaderboard.data.local.LeaderboardCredential
import com.ludoproof.game.feature.leaderboard.data.local.LeaderboardIdentityStore
import com.ludoproof.game.BuildConfig
import com.ludoproof.game.R
import com.google.android.gms.games.PlayGames
import com.ludoproof.game.feature.profile.data.local.ProfileStore
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import com.ludoproof.game.feature.profile.presentation.components.profileBadgesPanel
import com.ludoproof.game.feature.profile.presentation.components.profileIdentityPanel
import com.ludoproof.game.feature.profile.presentation.components.profileModeStatsPanel
import com.ludoproof.game.feature.profile.presentation.components.profilePurchasesPanel
import com.ludoproof.game.feature.profile.presentation.components.profileTopBar

class ProfileActivity : Activity() {
    internal var playGamesAuthenticated = false
        private set
    internal var playGamesSignInInProgress = false
        private set
    internal var playGamesDisplayName: String? = null
        private set
    internal var playGamesStatus = ""
        private set
    internal val isPlayGamesConfigured: Boolean
        get() = BuildConfig.PLAY_GAMES_PROJECT_ID.toLongOrNull()?.let { it > 0 } == true

    internal val profileStore by lazy {
        ProfileStore(this)
    }

    internal val accountRepository by lazy {
        (application as LudoProofApplication).accountRepository
    }
    internal var accountAuthInProgress = false
        private set
    internal var accountGoogleLinked = false
        private set
    internal var accountStatus = "Guest account"
        private set
    private val accountAuthExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "LudoPawsAccountAuth")
    }
    private val accountMainExecutor = Executor { command ->
        runOnUiThread { command.run() }
    }
    private var accountSwitchFromId: String? = null
    private var friendPresencePausedForAccountSwitch = false
    private var accountIdentityRecoveryPending = false
    internal val isAccountSignInConfigured: Boolean
        get() = BuildConfig.GOOGLE_SIGN_IN_WEB_CLIENT_ID.isNotBlank()

    override fun onCreate(
        savedInstanceState: Bundle?,
    ) {
        super.onCreate(
            savedInstanceState,
        )
        LudoProofTheme
            .configureWindow(
                this,
            )
        playGamesStatus = getString(R.string.profile_play_games_sign_in_prompt)
        renderProfile()
    }

    override fun onResume() {
        super.onResume()
        refreshPlayGamesSignIn()
        refreshAccountStatus()
    }

    override fun onRestart() {
        super.onRestart()
        renderProfile()
    }

    internal fun renderProfile() {
        val profile =
            profileStore
                .snapshot()

        val (root, host) =
            LudoProofTheme
                .arcadeRoot(
                    this,
                )

        val scroll =
            ScrollView(this).apply {
                isFillViewport =
                    true
                overScrollMode =
                    View.OVER_SCROLL_NEVER
            }
        val contentHost =
            FrameLayout(this)
        scroll.addView(
            contentHost,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        val horizontalPadding =
            dp(
                if (
                    LudoProofTheme
                        .isCompactWidth(
                            this,
                        )
                ) {
                    12
                } else {
                    18
                },
            )
        val maxWidth =
            dp(
                LudoProofTheme
                    .pageMaxContentWidthDp(
                        this,
                    ),
            )
        val width =
            minOf(
                resources
                    .displayMetrics
                    .widthPixels -
                    horizontalPadding *
                        2,
                maxWidth,
            )

        val content =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                setPadding(
                    0,
                    dp(12),
                    0,
                    dp(24),
                )
            }
        contentHost.addView(
            content,
            FrameLayout.LayoutParams(
                width,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or
                    Gravity.CENTER_HORIZONTAL,
            ),
        )

        host.addView(
            scroll,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )

        content.addView(
            profileTopBar(),
        )
        content.addView(
            profileIdentityPanel(profile),
            sectionParams(10),
        )
        content.addView(
            profileModeStatsPanel(profile),
            sectionParams(12),
        )
        content.addView(
            profileBadgesPanel(profile),
            sectionParams(12),
        )
        content.addView(
            profilePurchasesPanel(profile),
            sectionParams(12),
        )

        // This screen uses immersive full-screen layout. Respect the physical camera cutout
        // so both the centered title and Back button (and all content below) start in the safe area.
        root.setOnApplyWindowInsetsListener { _, insets ->
            val systemTopInset = insets.systemWindowInsetTop
            val cutoutTopInset =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val cutout = insets.displayCutout
                    maxOf(
                        cutout?.safeInsetTop ?: 0,
                        cutout?.boundingRects
                            ?.filter { it.top == 0 }
                            ?.maxOfOrNull { it.bottom }
                            ?: 0,
                    )
                } else {
                    0
                }
            val safeTopInset = maxOf(systemTopInset, cutoutTopInset)
            content.setPadding(
                0,
                maxOf(dp(12), safeTopInset + dp(8)),
                0,
                dp(24),
            )
            insets
        }

        setContentView(
            root,
        )
        root.requestApplyInsets()
    }

    internal fun openAccountActions() {
        if (accountAuthInProgress) return
        AlertDialog.Builder(this)
            .setTitle("Ludo Paws Account")
            .setMessage(
                if (accountGoogleLinked) {
                    "This account is saved to Google. You can restore it on another device, or choose another Google account only after confirming the switch."
                } else {
                    "Your guest account is currently tied to this installation. Save it to Google so your progress and eligible store items can follow you to another device."
                },
            )
            .setItems(arrayOf("Save this account to Google", "Restore an account saved to Google")) { _, which ->
                if (which == 0) {
                    startGoogleAccountFlow(linkCurrentAccount = true)
                } else {
                    confirmAccountRestore()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmAccountRestore() {
        AlertDialog.Builder(this)
            .setTitle("Restore another account?")
            .setMessage(
                "This will switch the active account on this device. The current guest profile, its Friend ID, leaderboard identity and inventory will not be merged into the restored account. If you have not saved the current guest to Google, cancel and save it first.",
            )
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Continue") { _, _ ->
                startGoogleAccountFlow(linkCurrentAccount = false)
            }
            .show()
    }

    private fun refreshAccountStatus() {
        if (accountAuthInProgress) return
        accountAuthExecutor.execute {
            try {
                val session = accountRepository.ensureGuestSession()
                val links = accountRepository.linkedIdentities()
                val linked = links.optBoolean("googleLinked")
                runOnUiThread {
                    if (isFinishing || isDestroyed || accountAuthInProgress) return@runOnUiThread
                    accountGoogleLinked = linked
                    accountStatus = if (linked) {
                        "Saved to Google · ${session.displayName}"
                    } else {
                        "Guest · save to keep progress"
                    }
                    renderProfile()
                }
            } catch (error: Exception) {
                runOnUiThread {
                    if (isFinishing || isDestroyed || accountAuthInProgress) return@runOnUiThread
                    accountStatus = "Account needs a connection"
                    renderProfile()
                }
            }
        }
    }

    private fun startGoogleAccountFlow(linkCurrentAccount: Boolean) {
        if (accountAuthInProgress) return
        if (!isAccountSignInConfigured) {
            AlertDialog.Builder(this)
                .setTitle("Google account setup required")
                .setMessage("Google account sign-in has not been configured for this build. The game can still be played as a guest, but account linking is not available yet.")
                .setPositiveButton("OK", null)
                .show()
            return
        }
        accountAuthInProgress = true
        if (!linkCurrentAccount) {
            accountSwitchFromId = accountRepository.currentSession()?.accountId
            (application as LudoProofApplication).pauseFriendPresenceForAccountSwitch()
            friendPresencePausedForAccountSwitch = true
        }
        accountStatus = "Preparing secure sign-in…"
        renderProfile()
        accountAuthExecutor.execute {
            try {
                // A restore must not depend on the old access/refresh token still being valid.
                // Create a guest first only when this install has no encrypted session at all.
                if (linkCurrentAccount || accountRepository.currentSession() == null) {
                    accountRepository.ensureGuestSession()
                }
                if (!linkCurrentAccount && accountSwitchFromId == null) {
                    accountSwitchFromId = accountRepository.currentSession()?.accountId
                }
                val nonce = accountRepository.googleChallenge()
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    launchGoogleCredentialPicker(nonce, linkCurrentAccount)
                }
            } catch (error: Exception) {
                finishAccountFlow(
                    success = false,
                    message = error.message ?: "The account service is not available right now.",
                )
            }
        }
    }

    private fun launchGoogleCredentialPicker(nonce: String, linkCurrentAccount: Boolean) {
        val option = GetGoogleIdOption.Builder()
            .setServerClientId(BuildConfig.GOOGLE_SIGN_IN_WEB_CLIENT_ID)
            .setNonce(nonce)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()
        CredentialManager.create(this).getCredentialAsync(
            this,
            request,
            null,
            accountMainExecutor,
            object : CredentialManagerCallback<GetCredentialResponse, GetCredentialException> {
                override fun onResult(result: GetCredentialResponse) {
                    val credential = result.credential
                    if (
                        credential !is CustomCredential ||
                        credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                    ) {
                        finishAccountFlow(false, "Google did not return a supported sign-in credential.")
                        return
                    }
                    val idToken = try {
                        GoogleIdTokenCredential.createFrom(credential.data).idToken
                    } catch (error: Exception) {
                        finishAccountFlow(false, "Google sign-in returned an invalid credential. Please try again.")
                        return
                    }
                    accountAuthExecutor.execute {
                        var restoreSessionChanged = false
                        var identityRecoveryCompleted = false
                        try {
                            if (linkCurrentAccount) {
                                accountRepository.linkGoogleIdentity(idToken, nonce)
                                // Keep existing social identities with the guest account only after
                                // each legacy credential has been independently validated by its service.
                                FriendIdentityStore(this@ProfileActivity)
                                    .load()?.let { accountRepository.linkFriendIdentity(it.friendToken) }
                                LeaderboardIdentityStore(this@ProfileActivity)
                                    .load()?.let { accountRepository.linkLeaderboardIdentity(it.profileToken) }
                            } else {
                                // Scope the existing local profile to the guest account before the
                                // active session is replaced; another account must not inherit its stats.
                                profileStore.prepareForAccountRestore()
                                accountRepository.restoreGoogleAccount(idToken, nonce)
                                restoreSessionChanged = true
                                val recovered = accountRepository.recoverLinkedIdentities()
                                val friendStore = FriendIdentityStore(this@ProfileActivity)
                                val friend = recovered.optJSONObject("friend")
                                if (friend != null) {
                                    friendStore.restoreFromAccount(
                                        FriendCredential(
                                            friendId = friend.getString("friendId"),
                                            friendToken = friend.getString("friendToken"),
                                            registrationRequestId = friend.getString("registrationRequestId"),
                                        ),
                                    )
                                } else {
                                    friendStore.clearAllForAccountSwitch()
                                }
                                val leaderboardStore = LeaderboardIdentityStore(this@ProfileActivity)
                                val leaderboard = recovered.optJSONObject("leaderboard")
                                if (leaderboard != null) {
                                    leaderboardStore.restoreFromAccount(
                                        LeaderboardCredential(
                                            profileId = leaderboard.getString("profileId"),
                                            profileToken = leaderboard.getString("profileToken"),
                                            registrationRequestId = leaderboard.getString("registrationRequestId"),
                                        ),
                                    )
                                } else {
                                    leaderboardStore.clearAllForAccountSwitch()
                                }
                                accountIdentityRecoveryPending = false
                                identityRecoveryCompleted = true
                            }
                            val session = accountRepository.currentSession()
                            val links = accountRepository.linkedIdentities()
                            runOnUiThread {
                                if (isFinishing || isDestroyed) return@runOnUiThread
                                accountAuthInProgress = false
                                if (friendPresencePausedForAccountSwitch && !accountIdentityRecoveryPending) {
                                    (application as LudoProofApplication).resumeFriendPresenceAfterAccountSwitch()
                                    friendPresencePausedForAccountSwitch = false
                                }
                                if (!accountIdentityRecoveryPending) accountSwitchFromId = null
                                accountGoogleLinked = links.optBoolean("googleLinked")
                                accountStatus = if (linkCurrentAccount) {
                                    "Saved to Google · ${session?.displayName ?: "Player"}"
                                } else {
                                    "Account restored · ${session?.displayName ?: "Player"}"
                                }
                                renderProfile()
                                AlertDialog.Builder(this@ProfileActivity)
                                    .setTitle(if (linkCurrentAccount) "Account saved" else "Account restored")
                                    .setMessage(
                                        if (linkCurrentAccount) {
                                            "Your current guest account is now linked to Google. Its identity was kept; another existing account was not merged."
                                        } else {
                                            "Your saved account is active on this device. If this device had a different guest account, its data has not been merged automatically."
                                        },
                                    )
                                    .setPositiveButton("OK", null)
                                    .show()
                            }
                        } catch (error: Exception) {
                            if (!linkCurrentAccount && !identityRecoveryCompleted &&
                                (restoreSessionChanged || accountRepository.currentSession()?.accountId != accountSwitchFromId)
                            ) {
                                // The server session changed but identity recovery did not complete.
                                // Clear old credentials and keep friend presence paused to prevent a
                                // freshly registered identity from replacing the linked one.
                                accountIdentityRecoveryPending = true
                                FriendIdentityStore(this@ProfileActivity).clearAllForAccountSwitch()
                                LeaderboardIdentityStore(this@ProfileActivity).clearAllForAccountSwitch()
                            }
                            val message = when (error) {
                                is AccountApiException -> when (error.code) {
                                    "GOOGLE_IDENTITY_ALREADY_LINKED" ->
                                        "This Google account is already linked to a different Ludo Paws account. Choose Restore instead; the accounts will not be merged automatically."
                                    "GOOGLE_IDENTITY_NOT_LINKED" ->
                                        "No saved Ludo Paws account is linked to that Google account yet. Choose Save this account to Google to secure the current guest."
                                    "ACCOUNT_IDENTITY_CONFLICT" ->
                                        "This account has a different Friend or leaderboard identity. To protect both profiles, they were not merged."
                                    else -> error.message
                                }
                                else -> error.message
                            } ?: "Account operation could not be completed."
                            finishAccountFlow(false, message)
                        }
                    }
                }

                override fun onError(e: GetCredentialException) {
                    finishAccountFlow(
                        false,
                        if (e.type.contains("cancel", ignoreCase = true)) {
                            "Google sign-in was cancelled. No account data was changed."
                        } else {
                            "Google sign-in could not be completed. Please try again."
                        },
                    )
                }
            },
        )
    }

    private fun finishAccountFlow(success: Boolean, message: String) {
        runOnUiThread {
            if (isFinishing || isDestroyed) return@runOnUiThread
            accountAuthInProgress = false
            if (friendPresencePausedForAccountSwitch && !accountIdentityRecoveryPending) {
                (application as LudoProofApplication).resumeFriendPresenceAfterAccountSwitch()
                friendPresencePausedForAccountSwitch = false
            }
            if (!accountIdentityRecoveryPending) accountSwitchFromId = null
            accountStatus = message.take(100)
            renderProfile()
            if (!success) {
                AlertDialog.Builder(this)
                    .setTitle("Ludo Paws Account")
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    internal fun beginPlayGamesSignIn() {
        if (!isPlayGamesConfigured) {
            playGamesStatus = getString(R.string.profile_play_games_setup_button_message)
            AlertDialog.Builder(this)
                .setTitle("Google Play Games")
                .setMessage(playGamesStatus)
                .setPositiveButton("OK", null)
                .show()
            return
        }
        if (playGamesSignInInProgress) return
        if (playGamesAuthenticated) {
            playGamesStatus =
                playGamesDisplayName?.let {
                    getString(R.string.profile_play_games_signed_in_as, it)
                } ?: getString(R.string.profile_play_games_connected)
            renderProfile()
            return
        }

        playGamesSignInInProgress = true
        playGamesStatus = getString(R.string.profile_play_games_sign_in_opening)
        renderProfile()
        PlayGames.getGamesSignInClient(this)
            .signIn()
            .addOnCompleteListener { task ->
                if (isFinishing || isDestroyed) return@addOnCompleteListener
                playGamesSignInInProgress = false
                if (!task.isSuccessful) {
                    Log.w(TAG, "Play Games sign-in failed", task.exception)
                    playGamesAuthenticated = false
                    playGamesDisplayName = null
                    playGamesStatus =
                        task.exception?.localizedMessage
                            ?.takeIf(String::isNotBlank)
                            ?.let {
                                getString(
                                    R.string.profile_play_games_sign_in_failed_detail,
                                    it,
                                )
                            }
                            ?: getString(R.string.profile_play_games_sign_in_failed)
                    renderProfile()
                    AlertDialog.Builder(this)
                        .setTitle("Google Play Games")
                        .setMessage(playGamesStatus)
                        .setPositiveButton("OK", null)
                        .show()
                    return@addOnCompleteListener
                }
                playGamesAuthenticated = task.result.isAuthenticated
                if (playGamesAuthenticated) {
                    loadPlayGamesPlayer()
                } else {
                    playGamesStatus =
                        getString(R.string.profile_play_games_sign_in_not_completed)
                    renderProfile()
                    AlertDialog.Builder(this)
                        .setTitle("Google Play Games")
                        .setMessage(playGamesStatus)
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
    }

    private fun refreshPlayGamesSignIn() {
        if (!isPlayGamesConfigured) {
            playGamesAuthenticated = false
            playGamesDisplayName = null
            playGamesStatus =
                getString(R.string.profile_play_games_setup_required)
            renderProfile()
            return
        }
        if (playGamesSignInInProgress) return
        PlayGames.getGamesSignInClient(this)
            .isAuthenticated()
            .addOnCompleteListener { task ->
                if (isFinishing || isDestroyed) return@addOnCompleteListener
                if (!task.isSuccessful) {
                    Log.w(TAG, "Could not check Play Games sign-in state", task.exception)
                    playGamesAuthenticated = false
                    playGamesStatus =
                        getString(R.string.profile_play_games_check_failed)
                    renderProfile()
                    Toast.makeText(this, playGamesStatus, Toast.LENGTH_LONG).show()
                    return@addOnCompleteListener
                }
                playGamesAuthenticated = task.result.isAuthenticated
                if (playGamesAuthenticated) {
                    loadPlayGamesPlayer()
                } else {
                    playGamesDisplayName = null
                    playGamesStatus = getString(R.string.profile_play_games_sign_in_prompt)
                    renderProfile()
                }
            }
    }

    private fun loadPlayGamesPlayer() {
        PlayGames.getPlayersClient(this)
            .currentPlayer
            .addOnCompleteListener { task ->
                if (isFinishing || isDestroyed) return@addOnCompleteListener
                if (!task.isSuccessful) {
                    Log.w(TAG, "Play Games authenticated, but player profile could not be loaded", task.exception)
                    playGamesDisplayName = null
                    playGamesStatus = getString(R.string.profile_play_games_connected)
                } else {
                    playGamesDisplayName =
                        task.result.displayName
                            ?.takeIf(String::isNotBlank)
                    playGamesDisplayName?.let { googleName ->
                        val savedName = profileStore.snapshot().displayName
                        if (savedName.isBlank() || savedName == "Guest User") {
                            profileStore.updateDisplayName(googleName)
                        }
                    }
                    playGamesStatus =
                        playGamesDisplayName?.let {
                            getString(R.string.profile_play_games_signed_in_as, it)
                        } ?: getString(R.string.profile_play_games_connected)
                }
                renderProfile()
            }
    }

    internal fun dp(
        value: Int,
    ): Int =
        LudoProofTheme
            .dp(
                this,
                value,
            )

    private fun sectionParams(
        topDp: Int,
    ):
        LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            topMargin =
                dp(
                    topDp,
                )
        }

    private companion object {
        const val TAG = "ProfilePlayGames"
    }
}
