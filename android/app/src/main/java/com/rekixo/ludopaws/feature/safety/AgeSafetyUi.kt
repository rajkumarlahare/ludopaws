package com.rekixo.ludopaws.feature.safety

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
        val saved = AgeSafetyPolicy.selectedAgeRange(activity)
        if (saved != null) {
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
                text = "Select the age range of the person who will play on this device. This choice stays on this device and is used to apply age-appropriate advertising and social-feature settings."
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
                onSelected()
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

    fun showSocialFeaturesRestricted(context: Context, onClose: () -> Unit = {}) {
        val activity = context as? Activity ?: return
        if (activity.isFinishing || activity.isDestroyed) return
        AlertDialog.Builder(activity)
            .setTitle("Friends features are unavailable")
            .setMessage(
                "For users who select an age range under 18, Friends invitations and Friend ID sharing, private messaging, microphone voice chat, and Quick Chat are disabled in this release. Public online board matches, computer opponents, and Pass & Play remain available. Social features will stay locked for under-18 users until in-app blocking, reporting, and moderation safeguards are implemented.",
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
                "This device is set to ${range.label}. Friends invitations, Friend ID sharing, private messages, voice chat, and Quick Chat are turned off for under-18 users in this release. This release does not offer an override for the social lock; these features will remain unavailable until in-app blocking, reporting, and moderation safeguards are implemented. Public online board gameplay and local modes remain available.",
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
