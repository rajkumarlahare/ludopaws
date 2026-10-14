package com.rekixo.ludopaws

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

/**
 * Branded launch screen reusing the exact shared background and Home hero art.
 * The progress indicator fills during the short launch transition, then opens Home.
 */
class SplashActivity : Activity() {
    private var progressAnimator: ValueAnimator? = null
    private var homeLaunchStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply the system splash theme before Activity creation so the OS icon
        // is suppressed and the branded WebP layout can draw as the first frame.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        LudoProofTheme.configureWindow(this)

        val root = FrameLayout(this).apply {
            setBackgroundColor(LudoProofTheme.NAVY_DARK)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }

        root.addView(
            ImageView(this).apply {
                setImageResource(R.drawable.ludo_paws_game_background)
                scaleType = ImageView.ScaleType.CENTER_CROP
                adjustViewBounds = false
                contentDescription = null
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            },
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )

        val heroWidthDp = minOf(
            (LudoProofTheme.screenWidthDp(this) - 28).coerceAtLeast(1),
            420,
        )
        val heroHeightDp = (heroWidthDp * HERO_HEIGHT_RATIO).toInt()
        root.addView(
            ImageView(this).apply {
                setImageResource(R.drawable.ludo_paws_home_hero)
                scaleType = ImageView.ScaleType.FIT_CENTER
                adjustViewBounds = false
                contentDescription = "Ludo Paws logo and characters"
            },
            FrameLayout.LayoutParams(
                LudoProofTheme.dp(this, heroWidthDp),
                LudoProofTheme.dp(this, heroHeightDp),
                Gravity.CENTER,
            ).apply {
                // Leave a clear lane at the bottom for the loading indicator.
                topMargin = -LudoProofTheme.dp(this@SplashActivity, 54)
            },
        )

        val loadingLabel = TextView(this).apply {
            text = "LOADING  0%"
            textSize = 12f
            setTypeface(android.graphics.Typeface.DEFAULT_BOLD)
            setTextColor(0xFFFFD45E.toInt())
            gravity = Gravity.CENTER
            includeFontPadding = false
            letterSpacing = 0.12f
            contentDescription = "Loading Ludo Paws"
        }

        val progress = ProgressBar(
            this,
            null,
            android.R.attr.progressBarStyleHorizontal,
        ).apply {
            isIndeterminate = false
            max = 100
            this.progress = 0
            progressTintList = ColorStateList.valueOf(0xFFFFD45E.toInt())
            progressBackgroundTintList = ColorStateList.valueOf(0xAA123B6B.toInt())
            minHeight = LudoProofTheme.dp(this@SplashActivity, 7)
            maxHeight = LudoProofTheme.dp(this@SplashActivity, 7)
            setPadding(0, 0, 0, 0)
            contentDescription = "App loading progress"
        }

        val loadingPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(
                LudoProofTheme.dp(this@SplashActivity, 8),
                LudoProofTheme.dp(this@SplashActivity, 8),
                LudoProofTheme.dp(this@SplashActivity, 8),
                LudoProofTheme.dp(this@SplashActivity, 8),
            )
            addView(
                loadingLabel,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LudoProofTheme.dp(this@SplashActivity, 22),
                ),
            )
            addView(
                progress,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LudoProofTheme.dp(this@SplashActivity, 7),
                ).apply {
                    topMargin = LudoProofTheme.dp(this@SplashActivity, 10)
                },
            )
        }

        root.addView(
            loadingPanel,
            FrameLayout.LayoutParams(
                LudoProofTheme.dp(this, minOf(heroWidthDp, 260)),
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
            ).apply {
                bottomMargin = LudoProofTheme.dp(this@SplashActivity, 74)
            },
        )

        setContentView(root)
        root.post {
            if (!isFinishing && !isDestroyed) {
                progressAnimator = ValueAnimator.ofInt(0, 100).apply {
                    duration = SPLASH_DURATION_MS
                    interpolator = LinearInterpolator()
                    addUpdateListener { animator ->
                        val value = animator.animatedValue as Int
                        progress.progress = value
                        loadingLabel.text = "LOADING  $value%"
                    }
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            openHome()
                        }
                    })
                    start()
                }
            }
        }
    }

    private fun openHome() {
        if (homeLaunchStarted || isFinishing || isDestroyed) return
        homeLaunchStarted = true
        startActivity(Intent(this, HomeActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    override fun onDestroy() {
        progressAnimator?.apply {
            // Cancelling during Activity teardown must not trigger a late Home launch.
            removeAllUpdateListeners()
            removeAllListeners()
            cancel()
        }
        progressAnimator = null
        super.onDestroy()
    }

    private companion object {
        const val SPLASH_DURATION_MS = 1_500L
        const val HERO_HEIGHT_RATIO = 302f / 388f
    }
}
