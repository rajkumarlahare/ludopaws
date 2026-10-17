package com.rekixo.ludopaws.feature.safety

import com.rekixo.ludopaws.HomeActivity
import android.widget.Toast
import android.content.Intent
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView

internal object AgeSafetyUi {
    /**
     * Neutral, full-window age screen: every band has equal visual weight and none is
     * preselected. Choice is saved locally before account bootstrap or ad requests.
     */
    fun showAgeRangeIfNeeded(activity: Activity, onSelected: () -> Unit) {
        if (AgeSafetyPolicy.hasSelectedAgeRange(activity)) {
            onSelected()
            return
        }
        if (activity.isFinishing || activity.isDestroyed) return

        val dialog = Dialog(activity)
        val scroll = ScrollView(activity).apply {
            isFillViewport = true
            setBackgroundColor(Color.WHITE)
        }
        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(activity, 28), dp(activity, 34), dp(activity, 28), dp(activity, 28))
            setBackgroundColor(Color.WHITE)
        }
        content.addView(
            TextView(activity).apply {
                text = "Choose your age range"
                textSize = 24f
                setTextColor(Color.rgb(26, 35, 48))
                gravity = Gravity.CENTER
            },
            fullWidthParams(),
        )
        content.addView(
            TextView(activity).apply {
                text = "Select the age range of the person who will play on this device. Your choice stays on this device and helps apply age-appropriate settings."
                textSize = 15f
                setTextColor(Color.rgb(68, 78, 91))
                gravity = Gravity.CENTER
                setPadding(0, dp(activity, 14), 0, dp(activity, 18))
            },
            fullWidthParams(),
        )

        val group = RadioGroup(activity).apply { orientation = RadioGroup.VERTICAL }
        val options = mutableMapOf<Int, AgeRange>()
        AgeRange.values().forEach { range ->
            val option = RadioButton(activity).apply {
                id = View.generateViewId()
                text = range.label
                textSize = 17f
                setTextColor(Color.rgb(32, 42, 55))
                setPadding(dp(activity, 6), dp(activity, 8), dp(activity, 6), dp(activity, 8))
                minHeight = dp(activity, 48)
            }
            options[option.id] = range
            group.addView(option, fullWidthParams())
        }
        content.addView(group, fullWidthParams())

        val continueButton = Button(activity).apply {
            text = "CONTINUE"
            isEnabled = false
            setOnClickListener {
                val selected = options[group.checkedRadioButtonId] ?: return@setOnClickListener
                AgeSafetyPolicy.setSelectedAgeRange(activity, selected)
                dialog.dismiss()
                if (selected.isUnder18) {
                    showUnder18Notice(activity, selected, onSelected)
                } else {
                    onSelected()
                }
            }
        }
        content.addView(
            continueButton,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 52)).apply {
                topMargin = dp(activity, 18)
            },
        )
        group.setOnCheckedChangeListener { _, checkedId ->
            continueButton.isEnabled = options.containsKey(checkedId)
        }

        scroll.addView(
            content,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )
        dialog.setContentView(scroll)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.WHITE))
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    fun requireAdultOnlineAccess(activity: Activity): Boolean {
        if (AgeSafetyPolicy.canUseOnlineFeatures(activity)) return true
        if (!AgeSafetyPolicy.hasSelectedAgeRange(activity)) {
            activity.startActivity(
                Intent(activity, HomeActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            )
        } else {
            Toast.makeText(
                activity,
                "Online features, purchases and ads are disabled for this age range. Offline play is available.",
                Toast.LENGTH_LONG,
            ).show()
        }
        activity.finish()
        return false
    }

    private fun showUnder18Notice(
        activity: Activity,
        range: AgeRange,
        onContinue: () -> Unit,
    ) {
        if (activity.isFinishing || activity.isDestroyed) return
        AlertDialog.Builder(activity)
            .setTitle("Offline play")
            .setMessage(
                "This device is set to ${range.label}. Online accounts and multiplayer, Friends invitations, private messaging, voice chat, Quick Chat, leaderboards, in-app purchases and advertising are unavailable in this age mode. Computer and Pass & Play modes remain available offline. Never share your name, contact details, school or location with strangers online.",
            )
            .setCancelable(false)
            .setPositiveButton("Continue") { _, _ -> onContinue() }
            .show()
    }

    fun showSocialFeaturesRestricted(context: Context, onClose: () -> Unit = {}) {
        val activity = context as? Activity ?: return
        if (activity.isFinishing || activity.isDestroyed) return
        AlertDialog.Builder(activity)
            .setTitle("Friends features are unavailable")
            .setMessage(
                "For users who select an age range under 18, online accounts and multiplayer, Friends invitations, private messaging, voice chat, Quick Chat, leaderboards, in-app purchases and ads are disabled in this release. Offline Computer and Pass & Play modes remain available.",
            )
            .setPositiveButton("OK") { _, _ -> onClose() }
            .setCancelable(false)
            .show()
    }

    fun showFamilySafetyInfo(context: Context) {
        val activity = context as? Activity ?: return
        val range = AgeSafetyPolicy.selectedAgeRange(activity) ?: return
        if (!range.isUnder18 || activity.isFinishing || activity.isDestroyed) return
        AlertDialog.Builder(activity)
            .setTitle("Family safety")
            .setMessage(
                "This device is set to ${range.label}. Online accounts and multiplayer, Friends invitations, private messages, voice chat, Quick Chat, leaderboards, purchases and ads are turned off for under-18 users in this release. Offline Computer and Pass & Play modes remain available.",
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun fullWidthParams() =
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )

    private fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density + 0.5f).toInt()
}
