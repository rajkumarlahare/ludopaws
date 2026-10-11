package com.ludoproof.game.feature.profile.presentation

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import com.ludoproof.game.LudoProofTheme
import com.ludoproof.game.BuildConfig
import com.ludoproof.game.R
import com.google.android.gms.games.PlayGames
import com.ludoproof.game.feature.profile.data.local.ProfileStore
import com.ludoproof.game.feature.profile.presentation.components.profileBadgesPanel
import com.ludoproof.game.feature.profile.presentation.components.profileIdentityPanel
import com.ludoproof.game.feature.profile.presentation.components.profileModeStatsPanel
import com.ludoproof.game.feature.profile.presentation.components.profilePawIdentityPanel
import com.ludoproof.game.feature.profile.presentation.components.profilePlayGamesPanel
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

    internal val profilePhotoUri: String?
        get() = getSharedPreferences(PHOTO_PREFS, MODE_PRIVATE)
            .getString(KEY_PROFILE_PHOTO_URI, null)

    internal fun chooseProfilePhoto() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        startActivityForResult(intent, REQUEST_PROFILE_PHOTO)
    }

    @Deprecated("Uses the platform activity result contract for compatibility with the existing Activity.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_PROFILE_PHOTO || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        runCatching {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        getSharedPreferences(PHOTO_PREFS, MODE_PRIVATE)
            .edit()
            .putString(KEY_PROFILE_PHOTO_URI, uri.toString())
            .apply()
        renderProfile()
    }

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
                    dp(34),
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
            profileIdentityPanel(
                profile,
            ),
            sectionParams(
                14,
            ),
        )
        content.addView(
            profilePlayGamesPanel(),
            sectionParams(12),
        )
        content.addView(
            profilePawIdentityPanel(),
            sectionParams(
                12,
            ),
        )
        content.addView(
            profileModeStatsPanel(
                profile,
            ),
            sectionParams(
                16,
            ),
        )
        content.addView(
            profileBadgesPanel(
                profile,
            ),
            sectionParams(
                16,
            ),
        )
        content.addView(
            profilePurchasesPanel(
                profile,
            ),
            sectionParams(
                16,
            ),
        )

        setContentView(
            root,
        )
    }

    internal fun beginPlayGamesSignIn() {
        if (!isPlayGamesConfigured) {
            playGamesStatus = getString(R.string.profile_play_games_setup_button_message)
            renderProfile()
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
                    return@addOnCompleteListener
                }
                playGamesAuthenticated = task.result.isAuthenticated
                if (playGamesAuthenticated) {
                    loadPlayGamesPlayer()
                } else {
                    playGamesStatus =
                        getString(R.string.profile_play_games_sign_in_not_completed)
                    renderProfile()
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
        const val REQUEST_PROFILE_PHOTO = 7412
        const val PHOTO_PREFS = "ludoproof_profile_photo"
        const val KEY_PROFILE_PHOTO_URI = "profile_photo_uri"
    }
}
