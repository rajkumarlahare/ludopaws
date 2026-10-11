package com.ludoproof.game.feature.team.presentation

import android.app.Activity
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.ludoproof.game.CachedMatchStore
import com.ludoproof.game.LudoPawsCharacterAvatarView
import com.ludoproof.game.core.ui.components.playSelectionPopAnimation
import com.ludoproof.game.GameMode
import com.ludoproof.game.GameModeIntent
import com.ludoproof.game.LudoProofTheme
import com.ludoproof.game.core.ui.components.AppFocusRing
import com.ludoproof.game.core.ui.art.plywoodBackground
import com.ludoproof.game.core.ui.art.playGamesSignInBackground
import com.ludoproof.game.core.ui.art.recessedPlywoodBackground
import com.ludoproof.game.core.ui.art.transparentDarkBlueStrokeBackground
import com.ludoproof.game.core.ui.icons.AppIconName
import com.ludoproof.game.core.ui.icons.AppIconView
import com.ludoproof.game.MainActivity
import com.ludoproof.game.PlayerSession
import com.ludoproof.game.SecureSessionStore
import com.ludoproof.game.feature.leaderboard.data.local.LeaderboardCredential
import com.ludoproof.game.feature.leaderboard.data.local.LeaderboardIdentityStore
import com.ludoproof.game.feature.leaderboard.data.remote.LeaderboardApi
import com.ludoproof.game.feature.characters.data.local.CharacterSelectionStore
import com.ludoproof.game.feature.characters.domain.catalog.LudoPawsCharacterCatalog
import com.ludoproof.game.feature.profile.data.local.ProfileStore
import com.ludoproof.game.feature.team.data.local.TeamMatchmakingStore
import com.ludoproof.game.feature.team.data.local.TeamMatchmakingTicket
import com.ludoproof.game.feature.team.data.remote.TeamMatchmakingApi
import com.ludoproof.game.ui.game.addGameTopActions
import org.json.JSONObject
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors

class TeamUpActivity : Activity() {
    private val api by lazy { TeamMatchmakingApi() }
    private val ticketStore by lazy { TeamMatchmakingStore(this) }
    private val identityStore by lazy { LeaderboardIdentityStore(this) }
    private val profileStore by lazy { ProfileStore(this) }
    private var selectedCharacterId = LudoPawsCharacterCatalog.DEFAULT_CHARACTER_ID
    private val sessionStore by lazy { SecureSessionStore(this) }
    private val cachedMatchStore by lazy { CachedMatchStore(this) }
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())

    @Volatile
    private var requestInFlight = false
    private var stopped = false

    private lateinit var quickTeamButton: FrameLayout
    private lateinit var cancelButton: Button
    private lateinit var setupContent: LinearLayout
    private lateinit var searchPanel: LinearLayout
    private lateinit var statusText: TextView
    private lateinit var slotsText: TextView
    private lateinit var timerText: TextView
    private lateinit var teamSlotProgressView: TeamSlotProgressView

    private val pollRunnable =
        object : Runnable {
            override fun run() {
                if (!stopped && ticketStore.load() != null) {
                    pollSearch()
                    updateTimer()
                }
                if (!stopped) {
                    handler.postDelayed(this, POLL_MS)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LudoProofTheme.configureWindow(this)
        selectedCharacterId = CharacterSelectionStore(this).load().characterId
        buildUi()
        renderSearchState()
    }

    override fun onStart() {
        super.onStart()
        stopped = false
        handler.removeCallbacks(pollRunnable)
        handler.post(pollRunnable)
    }

    override fun onStop() {
        stopped = true
        handler.removeCallbacks(pollRunnable)
        super.onStop()
    }

    override fun onDestroy() {
        stopped = true
        handler.removeCallbacksAndMessages(null)
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun buildUi() {
        val (root, host) = LudoProofTheme.arcadeRoot(this)
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(18), dp(12), dp(18), dp(30))
        }

        val headerTitle =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                addView(
                    LinearLayout(this@TeamUpActivity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER
                        addView(
                            AppIconView(
                                context = this@TeamUpActivity,
                                iconName = AppIconName.PEOPLE,
                                iconTint = LudoProofTheme.GOLD,
                            ),
                            LinearLayout.LayoutParams(dp(26), dp(26)).apply {
                                marginEnd = dp(8)
                            },
                        )
                        addView(
                            TextView(this@TeamUpActivity).apply {
                                text = "TEAM UP"
                                LudoProofTheme.title(this, 19f)
                                gravity = Gravity.CENTER
                            },
                        )
                    },
                )
                addView(
                    TextView(this@TeamUpActivity).apply {
                        text = "● ONLINE"
                        LudoProofTheme.body(this, 10f, centered = true, bright = true)
                        setTextColor(0xFF62E9FF.toInt())
                        setPadding(dp(12), dp(2), dp(12), dp(2))
                        background =
                            transparentDarkBlueStrokeBackground(
                                context = this@TeamUpActivity,
                                cornerRadiusDp = 999f,
                            )
                    },
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        gravity = Gravity.CENTER_HORIZONTAL
                    },
                )
            }
        content.addView(
            headerTitle,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(52),
            ),
        )
        content.addView(
            View(this),
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f,
            ),
        )

        val teamPanel =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(dp(18), dp(18), dp(18), dp(18))
                background =
                    plywoodBackground(
                        context = this@TeamUpActivity,
                        cornerRadiusDp = 24f,
                    )
            }
        setupContent = teamSetupPanel()
        teamPanel.addView(setupContent)

        quickTeamButton = FrameLayout(this).apply {
            isClickable = true
            isFocusable = true
            contentDescription = "Find a team online"
            background = playGamesSignInBackground(this@TeamUpActivity)

            val label = TextView(this@TeamUpActivity).apply {
                text = "FIND A TEAM"
                textSize = 18f
                setTypeface(Typeface.DEFAULT_BOLD)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                includeFontPadding = false
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
            addView(
                label,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                ),
            )

            val playIconSize = dp(36)
            val playIcon = ImageView(this@TeamUpActivity).apply {
                setImageDrawable(
                    context.getDrawable(com.ludoproof.game.R.drawable.ic_play_triangle_white)?.mutate(),
                )
                scaleType = ImageView.ScaleType.FIT_CENTER
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                translationX =
                    label.paint.measureText(label.text.toString()) / 2f + dp(6) + playIconSize / 2f
            }
            addView(
                playIcon,
                FrameLayout.LayoutParams(
                    playIconSize,
                    playIconSize,
                    Gravity.CENTER,
                ),
            )
            AppFocusRing.apply(this, cornerRadiusDp = 13f, strokeWidthDp = 3f)
            setOnClickListener { playSelectionPopAnimation { beginSearch() } }
        }
        setupContent.addView(
            quickTeamButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(60),
            ).apply { topMargin = dp(14) },
        )

        setupContent.addView(
            TextView(this).apply {
                text = "Find 3 players • Red + Yellow vs Green + Blue"
                LudoProofTheme.body(this, 11.5f, centered = true, bright = true)
                setPadding(dp(8), dp(8), dp(8), 0)
            },
        )

        searchPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(12), dp(14), dp(12), dp(14))
            // The search content is recessed into the outer plywood board.
            background =
                recessedPlywoodBackground(
                    context = this@TeamUpActivity,
                    outerCornerRadiusDp = 20f,
                )
            visibility = View.GONE

            addView(
                TextView(this@TeamUpActivity).apply {
                    text = "SEARCHING FOR TEAMMATES"
                    LudoProofTheme.title(this, 18f, gold = true)
                    gravity = Gravity.CENTER
                },
            )
            addView(
                TextView(this@TeamUpActivity).apply {
                    text = "Finding 3 players for your 2 vs 2 match"
                    LudoProofTheme.body(this, 11f, centered = true, bright = true)
                    setPadding(0, dp(4), 0, 0)
                },
            )
            addView(
                TextView(this@TeamUpActivity).apply {
                    text = "RED + YELLOW   VS   GREEN + BLUE"
                    LudoProofTheme.body(this, 9.5f, centered = true, bright = true)
                    setTextColor(0xFFBDEBFF.toInt())
                    setPadding(0, dp(7), 0, 0)
                },
            )

            teamSlotProgressView = TeamSlotProgressView(this@TeamUpActivity).apply {
                setQueuedPlayers(1, listOf(selectedCharacterId))
            }
            addView(
                teamSlotProgressView,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(108),
                ).apply { topMargin = dp(12) },
            )
            slotsText = TextView(this@TeamUpActivity).apply {
                text = "1 / 4 PLAYERS"
                LudoProofTheme.title(this, 18f)
                gravity = Gravity.CENTER
                setTextColor(0xFF69E8FF.toInt())
                setPadding(dp(8), dp(8), dp(8), dp(10))
            }
            addView(slotsText)

            timerText = TextView(this@TeamUpActivity).apply {
                text = "SEARCH  00:00"
                LudoProofTheme.body(this, 12f, centered = true, bright = true)
            }
            addView(timerText)

            cancelButton = Button(this@TeamUpActivity).apply {
                text = "CANCEL SEARCH"
                LudoProofTheme.danger(this)
                contentDescription = "Cancel team search"
                AppFocusRing.apply(this, cornerRadiusDp = 12f, strokeWidthDp = 2.5f)
                setOnClickListener { playSelectionPopAnimation { cancelSearch() } }
            }
            addView(
                cancelButton,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(52),
                ).apply { topMargin = dp(12) },
            )
        }
        teamPanel.addView(
            searchPanel,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        statusText = TextView(this).apply {
            text = "Quick Team is ready."
            LudoProofTheme.body(this, 11.5f, centered = true, bright = true)
            setPadding(dp(8), dp(12), dp(8), 0)
        }
        teamPanel.addView(statusText)

        content.addView(
            teamPanel,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(20) },
        )

        content.addView(
            TextView(this).apply {
                text = "SERVER-AUTHORITATIVE  •  VERIFIED DICE  •  TEAM RULESET V1"
                LudoProofTheme.body(this, 9.5f, centered = true)
                setPadding(dp(8), dp(8), dp(8), 0)
            },
        )
        content.addView(
            View(this),
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f,
            ),
        )

        scroll.addView(
            content,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )
        host.addView(
            scroll,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )
        val topActionsBar =
            addGameTopActions(
                host = host,
                context = this,
                onBack = { finish() },
                onSettings = {
                    com.ludoproof.game.ArcadeDialogs.showSettings(this)
                },
            ).apply {
                // Match the Online page's reusable plywood skin on Back and Settings.
                for (index in 0 until childCount) {
                    getChildAt(index).background =
                        plywoodBackground(
                            context = this@TeamUpActivity,
                            cornerRadiusDp = 999f,
                        )
                }
            }
        setContentView(root)
        root.setOnApplyWindowInsetsListener { _, insets ->
            val cutoutTop =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    insets.displayCutout?.safeInsetTop ?: 0
                } else {
                    0
                }
            val statusTop =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    insets.getInsetsIgnoringVisibility(WindowInsets.Type.statusBars()).top
                } else {
                    @Suppress("DEPRECATION")
                    insets.systemWindowInsetTop
                }
            val safeTopInset = maxOf(cutoutTop, statusTop)
            content.setPadding(
                    content.paddingLeft,
                    dp(12) + safeTopInset,
                    content.paddingRight,
                    content.paddingBottom,
            )
            topActionsBar.translationY = safeTopInset.toFloat()
            insets
        }
        root.requestApplyInsets()
    }

    /**
     * Uses the same square, recessed-plywood character tiles as Online Match.
     * The current player's chosen paw leads, followed by the other starter paws.
     */
    /**
     * Shows only the character chosen in the player's profile, using Online Match's
     * square recessed-plywood avatar treatment.
     */
    private fun teamCharacterTiles(): FrameLayout =
        FrameLayout(this).apply {
            val profileCharacterId =
                LudoPawsCharacterCatalog.canonicalCharacterId(selectedCharacterId)
                    ?: LudoPawsCharacterCatalog.DEFAULT_CHARACTER_ID
            val character =
                LudoPawsCharacterCatalog.character(profileCharacterId)
                    ?: LudoPawsCharacterCatalog.character(LudoPawsCharacterCatalog.DEFAULT_CHARACTER_ID)
                    ?: return@apply

            background = recessedPlywoodBackground(
                context = this@TeamUpActivity,
                outerCornerRadiusDp = 18f,
            )
            setPadding(dp(3), dp(3), dp(3), dp(3))
            contentDescription = "Your selected character: ${character.displayName}"
            addView(
                LudoPawsCharacterAvatarView(this@TeamUpActivity).apply {
                    bind(
                        characterId = character.id,
                        showCircularChrome = false,
                    )
                },
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                ),
            )
        }

    private fun teamSetupPanel(): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            addView(
                teamCharacterTiles(),
                LinearLayout.LayoutParams(dp(96), dp(96)).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                },
            )
            addView(
                TextView(this@TeamUpActivity).apply {
                    text = "2 VS 2 ONLINE"
                    LudoProofTheme.title(this, 26f, gold = false)
                    setTextColor(Color.WHITE)
                    gravity = Gravity.CENTER
                    setPadding(0, dp(7), 0, 0)
                },
            )
            addView(
                TextView(this@TeamUpActivity).apply {
                    text = "PLAY AS PARTNERS. WIN TOGETHER."
                    LudoProofTheme.body(this, 11f, centered = true, bright = true)
                    setTextColor(0xFFBDEBFF.toInt())
                    setPadding(0, dp(3), 0, dp(15))
                },
            )
            addView(teamLabelRow("TEAM A", "RED  +  YELLOW", 0xFFFF5D63.toInt(), 0xFFFFD324.toInt()))
            addView(
                teamLabelRow("TEAM B", "GREEN  +  BLUE", 0xFF35C83D.toInt(), 0xFF2A8CFF.toInt()),
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(46)).apply {
                    topMargin = dp(8)
                },
            )
        }

    private fun teamLabelRow(
        title: String,
        members: String,
        firstColor: Int,
        secondColor: Int,
    ): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(6), dp(12), dp(6))
            background =
                recessedPlywoodBackground(
                    context = this@TeamUpActivity,
                    outerCornerRadiusDp = 13f,
                )
            addView(TeamColorTokenView(this@TeamUpActivity, firstColor, secondColor), LinearLayout.LayoutParams(dp(38), dp(30)))
            addView(
                TextView(this@TeamUpActivity).apply {
                    text = title
                    LudoProofTheme.title(this, 11f, gold = true)
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(8), 0, dp(8), 0)
                },
                LinearLayout.LayoutParams(dp(74), LinearLayout.LayoutParams.WRAP_CONTENT),
            )
            addView(
                TextView(this@TeamUpActivity).apply {
                    text = members
                    LudoProofTheme.body(this, 13f, centered = true, bright = true)
                    setTypeface(Typeface.DEFAULT_BOLD)
                    gravity = Gravity.CENTER_VERTICAL
                },
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
            )
        }

    private fun beginSearch() {
        if (requestInFlight) return
        ticketStore.load()?.let {
            renderSearchState()
            pollSearch()
            return
        }

        // Refresh the persisted selection so the YOU portrait and matchmaking request agree.
        selectedCharacterId = CharacterSelectionStore(this).load().characterId

        val displayName =
            profileStore.snapshot().displayName
                .trim()
                .replace(Regex("\\s+"), " ")
                .take(24)
                .ifBlank { "Guest User" }
        if (displayName.length < 2) {
            showStatus("Set a valid profile name before Team Up.")
            return
        }

        val ticket = TeamMatchmakingTicket(
            requestId = UUID.randomUUID().toString(),
            displayName = displayName,
            startedAt = System.currentTimeMillis(),
        )
        runCatching { ticketStore.save(ticket) }
            .onFailure {
                showStatus("Could not save Team Up search. Try again.")
                return
            }

        renderSearchState()
        showStatus("Searching for 3 real players…")
        runRequest(
            action = {
                val credential = ensureLeaderboardCredential()
                api.search(
                    profileToken = credential.profileToken,
                    displayName = ticket.displayName,
                    clientRequestId = ticket.requestId,
                    characterId = selectedCharacterId,
                )
            },
            onSuccess = ::applyMatchmakingResponse,
        )
    }

    private fun pollSearch() {
        val ticket = ticketStore.load() ?: return
        runRequest(
            quietFailure = true,
            action = {
                val credential = ensureLeaderboardCredential()
                api.status(
                    profileToken = credential.profileToken,
                    clientRequestId = ticket.requestId,
                )
            },
            onSuccess = ::applyMatchmakingResponse,
        )
    }

    private fun cancelSearch() {
        val ticket = ticketStore.load()
            ?: run {
                renderSearchState()
                return
            }
        runRequest(
            action = {
                val credential = ensureLeaderboardCredential()
                api.cancel(
                    profileToken = credential.profileToken,
                    clientRequestId = ticket.requestId,
                )
            },
            onSuccess = { response ->
                if (response.optString("status") == "MATCHED") {
                    showStatus("Match was found while cancelling. Opening it…")
                    pollSearch()
                } else {
                    ticketStore.clear()
                    renderSearchState()
                    showStatus("Team search cancelled.")
                }
            },
        )
    }

    private fun applyMatchmakingResponse(response: JSONObject) {
        when (response.optString("status")) {
            "MATCHED" -> openMatchedGame(response)
            "SEARCHING" -> {
                val queued = response.optInt("queuedPlayers", 1).coerceIn(1, 4)
                renderQueuedPlayers(queued, queuedCharacterIds(response))
            }
            "IDLE" -> {
                ticketStore.clear()
                renderSearchState()
                showStatus("Search expired. Tap QUICK TEAM to search again.")
            }
            else -> showStatus("Unexpected Team Up matchmaking response.")
        }
    }

    private fun queuedCharacterIds(response: JSONObject): List<String> {
        val array = response.optJSONArray("queuedCharacterIds")
            ?: return listOf(selectedCharacterId)
        val received = buildList {
            for (index in 0 until minOf(array.length(), 4)) {
                val id = LudoPawsCharacterCatalog.canonicalCharacterId(array.optString(index))
                    ?: continue
                if (LudoPawsCharacterCatalog.character(id) != null) add(id)
            }
        }
        // The server orders the current request first; use the local profile value
        // for slot zero so YOU always matches the character used by this device.
        return buildList {
            add(selectedCharacterId)
            addAll(received.drop(1).take(3))
        }
    }

    private fun openMatchedGame(response: JSONObject) {
        val matchId = response.optString("matchId").trim().uppercase(Locale.ROOT)
        val playerId = response.optString("playerId").trim()
        val playerToken = response.optString("playerToken").trim()
        val matchMode = response.optString("matchMode")
        if (matchMode != "TEAM_UP") {
            showStatus("Team match validation failed. Search again.")
            return
        }

        val saved = runCatching {
            sessionStore.save(
                PlayerSession(
                    matchId = matchId,
                    playerId = playerId,
                    playerToken = playerToken,
                    modeWire = GameMode.TEAM_UP.wireValue,
                ),
            )
            cachedMatchStore.save(
                playerId,
                response.optJSONObject("state"),
            )
        }
        if (saved.isFailure) {
            sessionStore.clear()
            cachedMatchStore.clear()
            showStatus("Could not securely save the Team Up match.")
            return
        }

        ticketStore.clear()
        startActivity(
            Intent(this, MainActivity::class.java)
                .putExtra(
                    GameModeIntent.EXTRA_GAME_MODE,
                    GameMode.TEAM_UP.wireValue,
                )
                .putExtra(
                    GameModeIntent.EXTRA_RESUME_SAVED_MATCH,
                    true,
                ),
        )
        finish()
    }

    private fun ensureLeaderboardCredential(): LeaderboardCredential {
        identityStore.load()?.let { return it }
        val requestId = identityStore.registrationRequestId()
        val response = LeaderboardApi().registerProfile(requestId)
        val credential = LeaderboardCredential(
            profileId = response.getString("profileId"),
            profileToken = response.getString("profileToken"),
            registrationRequestId = requestId,
        )
        identityStore.save(credential)
        return credential
    }

    private fun runRequest(
        action: () -> JSONObject,
        onSuccess: (JSONObject) -> Unit,
        quietFailure: Boolean = false,
    ) {
        if (requestInFlight || executor.isShutdown || isFinishing || isDestroyed) return
        requestInFlight = true
        quickTeamButton.isEnabled = false
        if (::cancelButton.isInitialized) cancelButton.isEnabled = false

        val submitted = runCatching {
            executor.execute {
                try {
                    val response = action()
                    handler.post {
                        requestInFlight = false
                        if (isFinishing || isDestroyed) return@post
                        updateButtons()
                        onSuccess(response)
                    }
                } catch (error: Exception) {
                    handler.post {
                        requestInFlight = false
                        if (isFinishing || isDestroyed) return@post
                        updateButtons()
                        if (!quietFailure) {
                            showStatus(
                                "Team search error: " +
                                    (error.message ?: "connection failed"),
                            )
                        }
                    }
                }
            }
        }.isSuccess
        if (!submitted) {
            requestInFlight = false
            updateButtons()
        }
    }

    private fun renderSearchState() {
        val searching = ticketStore.load() != null
        setupContent.visibility = if (searching) View.GONE else View.VISIBLE
        searchPanel.visibility = if (searching) View.VISIBLE else View.GONE
        if (searching) {
            renderQueuedPlayers(1)
            updateTimer()
        }
        updateButtons()
    }

    private fun renderQueuedPlayers(
        queued: Int,
        characterIds: List<String> = listOf(selectedCharacterId),
    ) {
        val safe = queued.coerceIn(1, 4)
        slotsText.text = "$safe / 4 PLAYERS"
        teamSlotProgressView.setQueuedPlayers(safe, characterIds)
        if (safe >= 4) {
            showStatus("Match found • assigning opposite-color teams…")
        } else {
            showStatus("Searching • ${4 - safe} more player" + if (4 - safe == 1) "" else "s" + " needed.")
        }
    }

    private class TeamColorTokenView(
        context: android.content.Context,
        private val firstColor: Int,
        private val secondColor: Int,
    ) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val radius = minOf(width * 0.30f, height * 0.42f)
            val firstX = width * 0.34f
            val secondX = width * 0.68f
            val cy = height * 0.48f
            drawToken(canvas, firstX, cy, radius, firstColor)
            drawToken(canvas, secondX, cy, radius, secondColor)
        }

        private fun drawToken(canvas: Canvas, x: Float, y: Float, radius: Float, color: Int) {
            paint.color = 0x88020A18.toInt()
            canvas.drawCircle(x + dp(1f), y + dp(2f), radius * 1.18f, paint)
            paint.shader =
                android.graphics.RadialGradient(
                    x - radius * 0.35f,
                    y - radius * 0.38f,
                    radius * 1.6f,
                    intArrayOf(0xFFFFFFFF.toInt(), color, 0xFF06152F.toInt()),
                    null,
                    android.graphics.Shader.TileMode.CLAMP,
                )
            canvas.drawCircle(x, y, radius, paint)
            paint.shader = null
        }

        private fun dp(value: Float): Float = value * resources.displayMetrics.density
    }

    /**
     * Shows the server-confirmed queue as a sequential connection animation:
     * YOU -> next open slot. The next slot's ring orbits while we wait. When the
     * server confirms another queued player, that slot locks in and the scan
     * extends from it toward the following slot.
     */
    private class TeamSlotProgressView(context: android.content.Context) : FrameLayout(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val playerColors =
            intArrayOf(
                0xFFFF5D63.toInt(),
                0xFFFFD324.toInt(),
                0xFF35C83D.toInt(),
                0xFF2A8CFF.toInt(),
            )
        private var queuedPlayers = 1
        private var queuedCharacterIds: List<String> = emptyList()
        private var approachStartedAt = 0L
        private val characterViews = Array(4) {
            LudoPawsCharacterAvatarView(context).apply {
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                bind(
                    characterId = LudoPawsCharacterCatalog.DEFAULT_CHARACTER_ID,
                    showCircularChrome = false,
                )
                visibility = View.INVISIBLE
            }
        }

        init {
            setWillNotDraw(false)
            characterViews.forEach { avatar ->
                addView(
                    avatar,
                    FrameLayout.LayoutParams(dp(29f).toInt(), dp(29f).toInt()),
                )
            }
        }

        fun setQueuedPlayers(value: Int, characterIds: List<String>) {
            val next = value.coerceIn(1, 4)
            val safeCharacterIds = characterIds.take(4)
            val previousQueuedPlayers = queuedPlayers
            val previousCharacterIds = queuedCharacterIds
            if (
                approachStartedAt == 0L ||
                next != queuedPlayers ||
                safeCharacterIds != queuedCharacterIds
            ) {
                approachStartedAt = SystemClock.uptimeMillis()
            }
            queuedPlayers = next
            queuedCharacterIds = safeCharacterIds

            characterViews.forEachIndexed { index, avatar ->
                val characterId = safeCharacterIds.getOrNull(index)
                if (index < next && characterId != null) {
                    val newlyFilledSlot =
                        index >= previousQueuedPlayers ||
                            previousCharacterIds.getOrNull(index) != characterId
                    avatar.visibility = View.VISIBLE
                    avatar.bind(
                        characterId = characterId,
                        ringColor = 0xFF62E8FF.toInt(),
                        showCircularChrome = false,
                    )
                    if (newlyFilledSlot) avatar.playSelectionPopAnimation()
                } else {
                    avatar.visibility = View.INVISIBLE
                }
            }
            requestLayout()
            invalidate()
        }

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            if (queuedPlayers < 4 && visibility == View.VISIBLE) {
                if (approachStartedAt == 0L) {
                    approachStartedAt = SystemClock.uptimeMillis()
                }
                postInvalidateOnAnimation()
            }
        }

        override fun onVisibilityChanged(changedView: View, visibility: Int) {
            super.onVisibilityChanged(changedView, visibility)
            if (visibility == View.VISIBLE && queuedPlayers < 4 && isAttachedToWindow) {
                if (approachStartedAt == 0L) {
                    approachStartedAt = SystemClock.uptimeMillis()
                }
                postInvalidateOnAnimation()
            }
        }

        override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
            val inset = dp(5f)
            val contentRight = width - inset
            val spacing = (contentRight - inset) / 4f
            val centerY = height * 0.39f
            val avatarSize = dp(29f).toInt()
            for (index in characterViews.indices) {
                val centerX = inset + spacing * (index + 0.5f)
                val childLeft = (centerX - avatarSize / 2f).toInt()
                val childTop = (centerY - avatarSize / 2f).toInt()
                characterViews[index].layout(
                    childLeft,
                    childTop,
                    childLeft + avatarSize,
                    childTop + avatarSize,
                )
            }
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            if (width <= 0 || height <= 0) return

            val now = SystemClock.uptimeMillis()
            if (approachStartedAt == 0L) approachStartedAt = now

            val inset = dp(5f)
            val left = inset
            val right = width - inset
            val spacing = (right - left) / 4f
            val radius = minOf(dp(19f), height * 0.25f)
            val cy = height * 0.39f
            val centers = FloatArray(4) { index -> left + spacing * (index + 0.5f) }

            val targetIndex = queuedPlayers
            val sourceIndex = (queuedPlayers - 1).coerceIn(0, 3)
            val isSearching = queuedPlayers < 4 && isAttachedToWindow && visibility == View.VISIBLE
            val elapsed = (now - approachStartedAt).coerceAtLeast(0L)
            val approachFraction =
                (elapsed / APPROACH_DURATION_MS.toFloat()).coerceIn(0f, 1f)

            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.ROUND
            paint.strokeWidth = dp(3f)
            for (index in 0..2) {
                paint.color = when {
                    index < queuedPlayers - 1 -> 0xFFFFD34A.toInt()
                    index == queuedPlayers - 1 && queuedPlayers < 4 -> 0x6657DFFF
                    else -> 0x665D87B4
                }
                canvas.drawLine(centers[index], cy, centers[index + 1], cy, paint)
            }

            if (isSearching) {
                val sourceX = centers[sourceIndex]
                val targetX = centers[targetIndex]
                val targetEdgeX = targetX - radius - dp(5f)
                val reachedX = sourceX + (targetEdgeX - sourceX) * approachFraction
                paint.color = 0xFF59DEFF.toInt()
                paint.strokeWidth = dp(2.8f)
                canvas.drawLine(sourceX, cy, reachedX, cy, paint)
                paint.style = Paint.Style.FILL
                paint.color = 0x8859DEFF.toInt()
                canvas.drawCircle(reachedX, cy, dp(5f), paint)
                paint.color = 0xFFE5FCFF.toInt()
                canvas.drawCircle(reachedX, cy, dp(2.3f), paint)
            }

            // Confirmed players get their production character portraits as child views.
            // Empty slots retain a neutral placeholder silhouette only.
            for (index in 0..3) {
                val x = centers[index]
                val isQueued = index < queuedPlayers
                val isTarget = isSearching && index == targetIndex
                paint.style = Paint.Style.FILL
                paint.color = if (isQueued) playerColors[index] else 0xFF132A4A.toInt()
                canvas.drawCircle(x, cy, radius, paint)

                paint.style = Paint.Style.STROKE
                paint.strokeWidth = dp(if (isTarget) 2.6f else 2f)
                paint.color = when {
                    isQueued -> 0xFFFFF4D1.toInt()
                    isTarget -> 0xFF57DFFF.toInt()
                    else -> 0xFF8CA9C7.toInt()
                }
                canvas.drawCircle(x, cy, radius, paint)

                if (!isQueued || queuedCharacterIds.getOrNull(index) == null) {
                    drawPlayerPlaceholder(canvas, x, cy, radius, isQueued)
                }
            }

            if (isSearching && approachFraction >= 1f) {
                val orbitRadius = radius + dp(4.5f)
                val arcBounds = android.graphics.RectF(
                    centers[targetIndex] - orbitRadius,
                    cy - orbitRadius,
                    centers[targetIndex] + orbitRadius,
                    cy + orbitRadius,
                )
                val angle = (now % ORBIT_DURATION_MS).toFloat() / ORBIT_DURATION_MS.toFloat() * 360f - 90f
                val sweep = 96f
                paint.style = Paint.Style.STROKE
                paint.strokeCap = Paint.Cap.ROUND
                paint.strokeWidth = dp(3.2f)
                paint.color = 0xFF62E8FF.toInt()
                canvas.drawArc(arcBounds, angle, sweep, false, paint)
                val tipAngle = Math.toRadians((angle + sweep).toDouble())
                val tipX = centers[targetIndex] + (kotlin.math.cos(tipAngle) * orbitRadius).toFloat()
                val tipY = cy + (kotlin.math.sin(tipAngle) * orbitRadius).toFloat()
                paint.style = Paint.Style.FILL
                paint.color = 0xFFE4FCFF.toInt()
                canvas.drawCircle(tipX, tipY, dp(2.6f), paint)
            }

            for (index in 0..3) {
                paint.color = if (index < queuedPlayers) 0xFFDDF4FF.toInt() else 0xFF8CA9C7.toInt()
                paint.textAlign = Paint.Align.CENTER
                paint.textSize = dp(8.5f)
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(
                    if (index == 0) "YOU" else if (index < queuedPlayers) "PLAYER ${index + 1}" else "OPEN",
                    centers[index],
                    height - dp(13f),
                    paint,
                )
            }

            if (isSearching) postInvalidateOnAnimation()
        }

        private fun drawPlayerPlaceholder(
            canvas: Canvas,
            x: Float,
            y: Float,
            radius: Float,
            isQueued: Boolean,
        ) {
            paint.color = if (isQueued) 0xFFFFF4D1.toInt() else 0xFF8CA9C7.toInt()
            paint.style = Paint.Style.FILL
            canvas.drawCircle(x, y - radius * 0.22f, radius * 0.22f, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = dp(2.5f)
            canvas.drawArc(
                x - radius * 0.48f,
                y - radius * 0.02f,
                x + radius * 0.48f,
                y + radius * 0.72f,
                200f,
                140f,
                false,
                paint,
            )
            paint.style = Paint.Style.FILL
        }

        private fun dp(value: Float): Float = value * resources.displayMetrics.density

        private companion object {
            const val APPROACH_DURATION_MS = 850L
            const val ORBIT_DURATION_MS = 1_250L
        }
    }

    private fun updateTimer() {
        if (!::timerText.isInitialized) return
        val startedAt = ticketStore.load()?.startedAt ?: return
        val elapsed = ((System.currentTimeMillis() - startedAt) / 1_000L).coerceAtLeast(0L)
        timerText.text = String.format(
            Locale.US,
            "SEARCH  %02d:%02d",
            elapsed / 60L,
            elapsed % 60L,
        )
    }

    private fun updateButtons() {
        val searching = ticketStore.load() != null
        quickTeamButton.isEnabled = !requestInFlight && !searching
        if (::cancelButton.isInitialized) {
            cancelButton.isEnabled = !requestInFlight && searching
        }
    }

    private fun showStatus(value: String) {
        statusText.text = value
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val POLL_MS = 2_000L
    }
}
