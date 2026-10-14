package com.rekixo.ludopaws.ui.dialogs

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

internal fun showPrivacyPolicyDialog(
    context: Context,
) {
    val dialog = baseDialog(context)
    val panel =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                settingsDp(context, 16),
                settingsDp(context, 14),
                settingsDp(context, 16),
                settingsDp(context, 14),
            )
            background =
                GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(
                        0xFF08378F.toInt(),
                        0xFF031B50.toInt(),
                    ),
                ).apply {
                    cornerRadius = settingsDp(context, 18).toFloat()
                    setStroke(
                        settingsDp(context, 2),
                        0xFFFFC735.toInt(),
                    )
                }

            addView(
                TextView(context).apply {
                    text = "PRIVACY POLICY"
                    textSize = 20f
                    setTypeface(Typeface.DEFAULT_BOLD)
                    setTextColor(0xFFFFD34E.toInt())
                    gravity = Gravity.CENTER
                },
            )
        }

    val scroll =
        ScrollView(context).apply {
            isFillViewport = false
            overScrollMode = ScrollView.OVER_SCROLL_IF_CONTENT_SCROLLS
        }
    val body =
        TextView(context).apply {
            text = PRIVACY_POLICY_TEXT
            textSize = 13.5f
            setTextColor(Color.WHITE)
            setLineSpacing(0f, 1.18f)
            setPadding(
                settingsDp(context, 4),
                settingsDp(context, 14),
                settingsDp(context, 4),
                settingsDp(context, 14),
            )
        }
    scroll.addView(body)
    panel.addView(
        scroll,
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            settingsDp(context, 430),
        ),
    )

    panel.addView(
        TextView(context).apply {
            text = "OPEN FULL ONLINE PRIVACY POLICY"
            textSize = 13f
            setTypeface(Typeface.DEFAULT_BOLD)
            setTextColor(0xFFFFD34E.toInt())
            gravity = Gravity.CENTER
            setPadding(
                0,
                settingsDp(context, 8),
                0,
                settingsDp(context, 8),
            )
            isClickable = true
            isFocusable = true
            contentDescription = "Open the full online Ludo Paws Privacy Policy"
            setOnClickListener {
                try {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://ludopaws.rekixo.com/privacy-policy.html"),
                        ),
                    )
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(context, "No browser is available to open this page.", Toast.LENGTH_LONG).show()
                }
            }
        },
    )

    panel.addView(
        TextView(context).apply {
            text = "CLOSE"
            textSize = 14f
            setTypeface(Typeface.DEFAULT_BOLD)
            setTextColor(0xFFFFD34E.toInt())
            gravity = Gravity.CENTER
            setPadding(
                0,
                settingsDp(context, 12),
                0,
                settingsDp(context, 8),
            )
            isClickable = true
            isFocusable = true
            setOnClickListener {
                dialog.dismiss()
            }
        },
    )

    dialog.setContentView(panel)
    sizeDialog(dialog, .88f)
    dialog.show()
}

private const val PRIVACY_POLICY_TEXT =
    """Last updated: October 12, 2026

Ludo Paws is published under the Rekixo brand. This policy describes information that may be processed by the Android game and its connected services. Which information is used depends on the features you choose and which services the publisher has enabled.

INFORMATION THAT MAY BE PROCESSED
• Account and profile information, such as an internal account identifier, display name, account/session status, and links to Google, Friend ID, or leaderboard identities when you choose to link those features.
• Online game and integrity information, such as match/player identifiers, moves, outcomes, dice/proof commitments, proof digests, and event history needed to operate multiplayer games and investigate game-integrity issues.
• Social and leaderboard information, such as Friend IDs, friend relationships, invitations/messages when used, and submitted player names, scores, wins, or points.
• Store and entitlement information, such as selected cosmetics, gem balance, cosmetics inventory, rewarded-ad progress when enabled, purchase product IDs, and transaction references used to verify entitlements and prevent duplicate grants.
• Device-local information, such as settings, local game state, selected appearance, and account-scoped cache data.
• Technical and security records needed to authenticate requests, maintain sessions, limit abuse, troubleshoot errors, and protect the service.

HOW INFORMATION IS USED AND SHARED
Information is used to provide account and game features, synchronize supported progress and entitlements, operate online matches and social/leaderboard features, verify game integrity and purchases, prevent abuse, troubleshoot service errors, and respond to privacy requests. Data may be processed by Google sign-in/Google Play Games, Google Play billing, advertising services such as AdMob when enabled, and infrastructure or network providers to the extent needed for configured features. Those providers also apply their own privacy notices and terms. Ludo Paws does not need your Google password to verify Google sign-in.

ON-DEVICE DATA, PURCHASES AND ADS
Game settings and some local profile or wallet information can be stored on your device for display and offline use; local caches are not authoritative proof of paid entitlements. Google Play handles its own order history and payment process. If ads or rewarded ads are enabled, Google Mobile Ads/AdMob may process ad requests and consent signals under Google's policies; rewards are applied only through the configured validation flow. This text does not mean ads or every online feature are currently enabled.

RETENTION AND ACCOUNT DELETION
Service data may be kept while needed to operate features, secure accounts, prevent abuse or duplicate grants, resolve disputes, maintain game integrity, or meet legal obligations. Any specific retention period and justified exception must be described in the official published policy. Use Profile → menu → Delete account and data to request deletion in the app. A guest-only account may use its optional delete-only guest code; save it privately before uninstalling. The official online deletion page is available at https://ludopaws.rekixo.com/account-deletion.html when the publisher-hosted site is published and available. Deleting a Ludo Paws account does not delete your Google account or erase Google's separate Play order history.

CHANGES AND CONTACT
This policy may be updated when the game, service providers, or applicable requirements change. Before public release, the publisher must verify the final provider list, data-retention details, support contact, and Play Console Data safety answers against the final app and server configuration."""
