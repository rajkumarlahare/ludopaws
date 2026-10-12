package com.ludoproof.game

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.google.android.gms.games.PlayGamesSdk
import com.ludoproof.game.feature.characters.data.local.CharacterSelectionStore
import com.ludoproof.game.feature.account.data.AccountRepository
import java.util.concurrent.Executors
import com.ludoproof.game.feature.friends.data.FriendPresenceController
import com.ludoproof.game.feature.settings.data.local.GameMusicController

class LudoProofApplication :
    Application(),
    Application.ActivityLifecycleCallbacks {
    private lateinit var friendPresence:
        FriendPresenceController
    val accountRepository: AccountRepository by lazy { AccountRepository(this) }
    private val accountBootstrapExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "LudoPawsAccountBootstrap")
    }
    private var resumedActivities = 0
    @Volatile
    private var friendPresencePausedForAccountSwitch = false

    override fun onCreate() {
        super.onCreate()
        PlayGamesSdk.initialize(this)
        // Prime the process-local cosmetic mirror before any remote create/join
        // request can be sent. Character selection remains presentation-only.
        CharacterSelectionStore(this).load()
        friendPresence =
            FriendPresenceController(this)
        // Create/restore the guest session off the UI thread. If the network is
        // unavailable, gameplay remains usable; monetized store actions fail closed.
        accountBootstrapExecutor.execute {
            runCatching { accountRepository.ensureGuestSession() }
        }
        registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityResumed(
        activity: Activity,
    ) {
        // OEMs may restore system bars after app switches, transient overlays,
        // or window-focus changes. Reassert the game presentation on resume.
        ImmersiveGameWindow.restore(activity)

        resumedActivities += 1
        if (resumedActivities == 1) {
            if (
                !friendPresencePausedForAccountSwitch &&
                !accountRepository.requiresIdentityRecovery()
            ) {
                friendPresence.start()
            }
            GameMusicController
                .onAppForeground(this)
        }
    }

    override fun onActivityPaused(
        activity: Activity,
    ) {
        resumedActivities =
            (resumedActivities - 1)
                .coerceAtLeast(0)
        if (resumedActivities == 0) {
            if (!friendPresencePausedForAccountSwitch) friendPresence.stop()
            GameMusicController
                .onAppBackground()
        }
    }

    fun pauseFriendPresenceForAccountSwitch() {
        friendPresencePausedForAccountSwitch = true
        friendPresence.stop()
    }

    fun resumeFriendPresenceAfterAccountSwitch() {
        friendPresencePausedForAccountSwitch = false
        if (resumedActivities > 0 && !accountRepository.requiresIdentityRecovery()) {
            friendPresence.start()
        }
    }

    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?,
    ) {
        // Keep every current and future Activity on the same edge-to-edge,
        // cutout-aware fullscreen contract without per-screen duplication.
        ImmersiveGameWindow.configure(activity)
    }

    override fun onActivityStarted(
        activity: Activity,
    ) = Unit

    override fun onActivityStopped(
        activity: Activity,
    ) = Unit

    override fun onActivitySaveInstanceState(
        activity: Activity,
        outState: Bundle,
    ) = Unit

    override fun onActivityDestroyed(
        activity: Activity,
    ) = Unit
}
