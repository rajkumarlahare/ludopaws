package com.ludoproof.game.feature.team.presentation

import android.app.Activity
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.ludoproof.game.CachedMatchStore
import com.ludoproof.game.GameMode
import com.ludoproof.game.GameModeIntent
import com.ludoproof.game.LudoProofTheme
import com.ludoproof.game.core.ui.components.AppFocusRing
import com.ludoproof.game.core.ui.icons.AppIconName
import com.ludoproof.game.core.ui.icons.AppIconView
import com.ludoproof.game.MainActivity
import com.ludoproof.game.PlayerSession
import com.ludoproof.game.SecureSessionStore
import com.ludoproof.game.feature.leaderboard.data.local.LeaderboardCredential
import com.ludoproof.game.feature.leaderboard.data.local.LeaderboardIdentityStore
import com.ludoproof.game.feature.leaderboard.data.remote.LeaderboardApi
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
    private val sessionStore by lazy { SecureSessionStore(this) }
    private val cachedMatchStore by lazy { CachedMatchStore(this) }
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())

    @Volatile
    private var requestInFlight = false
    private var stopped = false

    private lateinit var quickTeamButton: Button
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
                            LudoProofTheme.rounded(
                                0xCC071C49.toInt(),
                                999f,
                                0x6647D7FF,
                                1f,
                                this@TeamUpActivity,
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
                background = teamPanelBackground()
            }
        setupContent = teamSetupPanel()
        teamPanel.addView(setupContent)

        quickTeamButton = Button(this).apply {
            text = "▶   FIND A TEAM"
            textSize = 18f
            setTypeface(Typeface.DEFAULT_BOLD)
            setTextColor(0xFF342000.toInt())
            background =
                GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(0xFFFFD642.toInt(), 0xFFFF9D00.toInt()),
                ).apply {
                    cornerRadius = dp(999).toFloat()
                    setStroke(dp(2), 0xFFFFF0A0.toInt())
                }
            contentDescription = "Find a team online"
            AppFocusRing.apply(this, cornerRadiusDp = 999f, strokeWidthDp = 3f)
            setOnClickListener { beginSearch() }
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
            setPadding(0, dp(4), 0, 0)
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

            teamSlotProgressView = TeamSlotProgressView(this@TeamUpActivity)
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
                setOnClickListener { cancelSearch() }
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
            )
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

    private fun teamSetupPanel(): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            addView(
                TeamFormationView(this@TeamUpActivity),
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(116)),
            )
            addView(
                TextView(this@TeamUpActivity).apply {
                    text = "2 VS 2 ONLINE"
                    LudoProofTheme.title(this, 26f, gold = true)
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
            val ruleRow =
                LinearLayout(this@TeamUpActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER
                    setPadding(0, dp(14), 0, 0)
                    addView(ruleChip("NO FRIENDLY CAPTURE"))
                    addView(
                        ruleChip("BOTH PARTNERS FINISH"),
                        LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(34)).apply {
                            marginStart = dp(7)
                        },
                    )
                }
            addView(ruleRow)
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
                GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    intArrayOf(0xCC071A3A.toInt(), 0xAA0A2B58.toInt()),
                ).apply {
                    cornerRadius = dp(13).toFloat()
                    setStroke(dp(1), 0x4459D9FF)
                }
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

    private fun ruleChip(value: String): TextView =
        TextView(this).apply {
            text = value
            LudoProofTheme.body(this, 8.5f, centered = true, bright = true)
            setTypeface(Typeface.DEFAULT_BOLD)
            setPadding(dp(8), dp(7), dp(8), dp(7))
            background =
                LudoProofTheme.rounded(
                    0xAA071A3A.toInt(),
                    999f,
                    0x5569DFFF,
                    1f,
                    this@TeamUpActivity,
                )
        }

    private fun teamPanelBackground(): GradientDrawable =
        GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(0xF20A2856.toInt(), 0xF2081B3D.toInt()),
        ).apply {
            cornerRadius = dp(22).toFloat()
            setStroke(dp(2), 0xFF40CFFF.toInt())
        }

    private fun beginSearch() {
        if (requestInFlight) return
        ticketStore.load()?.let {
            renderSearchState()
            pollSearch()
            return
        }

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
                renderQueuedPlayers(queued)
            }
            "IDLE" -> {
                ticketStore.clear()
                renderSearchState()
                showStatus("Search expired. Tap QUICK TEAM to search again.")
            }
            else -> showStatus("Unexpected Team Up matchmaking response.")
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

    private fun renderQueuedPlayers(queued: Int) {
        val safe = queued.coerceIn(1, 4)
        slotsText.text = "$safe / 4 PLAYERS"
        teamSlotProgressView.setQueuedPlayers(safe)
        if (safe >= 4) {
            showStatus("Match found • assigning opposite-color teams…")
        } else {
            showStatus("Searching • ${4 - safe} more player" + if (4 - safe == 1) "" else "s" + " needed.")
        }
    }

    private class TeamFormationView(context: android.content.Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val outline =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = dp(1.5f)
            }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val centerX = width / 2f
            val centerY = height * 0.52f
            val radius = minOf(width * 0.36f, height * 0.47f)

            paint.shader =
                android.graphics.RadialGradient(
                    centerX,
                    centerY,
                    radius,
                    intArrayOf(0xFF168AF4.toInt(), 0xFF06275F.toInt()),
                    null,
                    android.graphics.Shader.TileMode.CLAMP,
                )
            canvas.drawCircle(centerX, centerY, radius, paint)
            paint.shader = null
            outline.color = 0xFFFFD34A.toInt()
            outline.strokeWidth = dp(3f)
            canvas.drawCircle(centerX, centerY, radius, outline)
            outline.color = 0xAAE7F8FF.toInt()
            outline.strokeWidth = dp(1f)
            canvas.drawCircle(centerX, centerY, radius - dp(5f), outline)

            val people =
                listOf(
                    Person(centerX - radius * 0.52f, centerY + radius * 0.09f, 0xFFFF5D63.toInt(), 0.72f),
                    Person(centerX + radius * 0.52f, centerY + radius * 0.09f, 0xFFFFD324.toInt(), 0.72f),
                    Person(centerX - radius * 0.18f, centerY - radius * 0.34f, 0xFF35C83D.toInt(), 1f),
                    Person(centerX + radius * 0.18f, centerY - radius * 0.34f, 0xFF2A8CFF.toInt(), 1f),
                )
            people.forEach { person ->
                drawPerson(canvas, person.x, person.y, radius * 0.20f * person.scale, person.color)
            }
            outline.color = 0xFFFFD34A.toInt()
            outline.strokeWidth = dp(3f)
            canvas.drawLine(
                centerX - radius * 1.05f,
                centerY + radius * 0.42f,
                centerX - radius * 0.79f,
                centerY + radius * 0.42f,
                outline,
            )
            canvas.drawLine(
                centerX + radius * 0.79f,
                centerY + radius * 0.42f,
                centerX + radius * 1.05f,
                centerY + radius * 0.42f,
                outline,
            )
        }

        private fun drawPerson(
            canvas: Canvas,
            x: Float,
            y: Float,
            headRadius: Float,
            color: Int,
        ) {
            val light = blend(color, Color.WHITE, 0.34f)
            val dark = blend(color, 0xFF06152F.toInt(), 0.24f)
            val shoulder = headRadius * 1.18f
            val bodyTop = y + headRadius * 0.78f
            val bodyBottom = y + headRadius * 2.55f
            val body =
                Path().apply {
                    moveTo(x - shoulder * 0.48f, bodyTop)
                    cubicTo(x - shoulder, bodyTop, x - shoulder * 1.02f, bodyBottom, x - shoulder * 0.80f, bodyBottom)
                    lineTo(x + shoulder * 0.80f, bodyBottom)
                    cubicTo(x + shoulder * 1.02f, bodyBottom, x + shoulder, bodyTop, x + shoulder * 0.48f, bodyTop)
                    close()
                }
            paint.shader =
                android.graphics.LinearGradient(
                    x - shoulder,
                    bodyTop,
                    x + shoulder,
                    bodyBottom,
                    intArrayOf(light, color, dark),
                    null,
                    android.graphics.Shader.TileMode.CLAMP,
                )
            canvas.drawPath(body, paint)
            paint.shader = null
            outline.color = blend(color, 0xFF06152F.toInt(), 0.30f)
            outline.strokeWidth = dp(1.5f)
            canvas.drawPath(body, outline)
            paint.shader =
                android.graphics.RadialGradient(
                    x - headRadius * 0.36f,
                    y - headRadius * 0.38f,
                    headRadius * 1.55f,
                    intArrayOf(light, color, dark),
                    null,
                    android.graphics.Shader.TileMode.CLAMP,
                )
            canvas.drawCircle(x, y, headRadius, paint)
            paint.shader = null
            outline.color = blend(color, 0xFF06152F.toInt(), 0.30f)
            canvas.drawCircle(x, y, headRadius, outline)
        }

        private fun blend(first: Int, second: Int, amount: Float): Int =
            android.graphics.Color.rgb(
                (android.graphics.Color.red(first) * (1f - amount) + android.graphics.Color.red(second) * amount).toInt(),
                (android.graphics.Color.green(first) * (1f - amount) + android.graphics.Color.green(second) * amount).toInt(),
                (android.graphics.Color.blue(first) * (1f - amount) + android.graphics.Color.blue(second) * amount).toInt(),
            )

        private fun dp(value: Float): Float = value * resources.displayMetrics.density

        private data class Person(val x: Float, val y: Float, val color: Int, val scale: Float)
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

    private class TeamSlotProgressView(context: android.content.Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val playerColors =
            intArrayOf(
                0xFFFF5D63.toInt(),
                0xFFFFD324.toInt(),
                0xFF35C83D.toInt(),
                0xFF2A8CFF.toInt(),
            )
        private var queuedPlayers = 1

        fun setQueuedPlayers(value: Int) {
            queuedPlayers = value.coerceIn(1, 4)
            invalidate()
        }

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            if (visibility == View.VISIBLE && queuedPlayers < 4) {
                postInvalidateOnAnimation()
            }
        }

        override fun onVisibilityChanged(changedView: View, visibility: Int) {
            super.onVisibilityChanged(changedView, visibility)
            if (visibility == View.VISIBLE && queuedPlayers < 4 && isAttachedToWindow) {
                postInvalidateOnAnimation()
            }
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val inset = dp(5f)
            val left = inset
            val right = width - inset
            val spacing = (right - left) / 4f
            val radius = minOf(dp(19f), height * 0.25f)
            val cy = height * 0.39f

            paint.strokeCap = Paint.Cap.ROUND
            paint.strokeWidth = dp(3f)
            for (index in 0..2) {
                paint.color = if (index + 1 < queuedPlayers) 0xFFFFD34A.toInt() else 0x665D87B4
                canvas.drawLine(
                    left + spacing * (index + 1) + radius * 0.72f,
                    cy,
                    left + spacing * (index + 2) - radius * 0.72f,
                    cy,
                    paint,
                )
            }

            if (queuedPlayers < 4 && isAttachedToWindow && visibility == View.VISIBLE) {
                val phase = (SystemClock.uptimeMillis() % 1_500L) / 1_500f
                val scanX = left + phase * (right - left)
                paint.color = 0x5539DFFF.toInt()
                canvas.drawCircle(scanX, cy, dp(12f), paint)
                paint.color = 0xFFBDF7FF.toInt()
                canvas.drawCircle(scanX, cy, dp(3.5f), paint)
            }

            for (index in 0..3) {
                val x = left + spacing * (index + 0.5f)
                val isQueued = index < queuedPlayers
                val pulse = (SystemClock.uptimeMillis() % 1_200L) / 1_200f
                if (!isQueued && index == queuedPlayers && isAttachedToWindow && visibility == View.VISIBLE) {
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = dp(2f)
                    paint.color = 0xAA57DFFF.toInt()
                    canvas.drawCircle(x, cy, radius + dp(4f + pulse * 7f), paint)
                    paint.style = Paint.Style.FILL
                }
                paint.color = if (isQueued) playerColors[index] else 0xFF132A4A.toInt()
                canvas.drawCircle(x, cy, radius, paint)
                paint.color = if (isQueued) 0xFFFFF4D1.toInt() else 0xFF8CA9C7.toInt()
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = dp(2f)
                canvas.drawCircle(x, cy, radius, paint)
                paint.style = Paint.Style.FILL
                drawPlayerIcon(canvas, x, cy, radius, isQueued)

                paint.color = if (isQueued) 0xFFDDF4FF.toInt() else 0xFF8CA9C7.toInt()
                paint.textAlign = Paint.Align.CENTER
                paint.textSize = dp(8.5f)
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(
                    if (index == 0) "YOU" else if (isQueued) "PLAYER ${index + 1}" else "OPEN",
                    x,
                    height - dp(13f),
                    paint,
                )
            }

            if (queuedPlayers < 4 && isAttachedToWindow && visibility == View.VISIBLE) {
                postInvalidateOnAnimation()
            }
        }

        private fun drawPlayerIcon(
            canvas: Canvas,
            x: Float,
            y: Float,
            radius: Float,
            isQueued: Boolean,
        ) {
            paint.color = if (isQueued) 0xFFFFF4D1.toInt() else 0xFF8CA9C7.toInt()
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
