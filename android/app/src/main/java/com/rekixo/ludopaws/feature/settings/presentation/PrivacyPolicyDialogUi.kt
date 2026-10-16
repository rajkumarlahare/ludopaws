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
            text = "OPEN PRICING & IN-APP PURCHASE POLICY"
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
            contentDescription = "Open the Ludo Paws pricing and in-app purchase policy"
            setOnClickListener {
                try {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://ludopaws.rekixo.com/pricing.html"),
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
            text = "OPEN TERMS OF SERVICE"
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
            contentDescription = "Open the Ludo Paws Terms of Service"
            setOnClickListener {
                try {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://ludopaws.rekixo.com/terms-of-service.html"),
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
    """Last updated: October 16, 2026

Ludo Paws is a mobile Ludo-style game published under the Rekixo brand. The information processed depends on the features you use and which optional services are enabled.

INFORMATION THAT MAY BE PROCESSED
• Account and identity: internal account ID, display name, account state, and links to Google, Friend ID or leaderboard identities when you use those features. Ludo Paws does not ask for your Google password.
• Authentication and security: session identifiers and expiry/revocation state, token hashes, sign-in nonces, request IDs and limited anti-abuse/rate-limit records.
• Gameplay and fairness: online match IDs, pseudonymous player IDs, game state, moves, results, timestamps, ruleset IDs, dice commitments, proof digests and event history. Offline game state and progression may remain on your device.
• Friends and social: Friend IDs, display names, requests, relationships, invitations, recent-player entries, conversations and leaderboard results when those features are used.
• Optional voice chat: if you start voice chat during a Friends match and grant microphone permission, microphone audio is sent to participating players over WebRTC. The game API relays connection setup data; the current client does not upload the voice stream to the game API for recording or storage. Network information may be visible to connection providers or other participants depending on the route.
• Store and rewards: Gem balance, owned/selected cosmetics, reward progress when enabled, Google Play product IDs and hashed purchase references used to verify entitlements and prevent duplicate grants. Google Play handles payment details.
• Device and technical data: local settings, offline state, profile statistics, account cache, and technical connection information needed to deliver and protect the service. The API derives a one-way IP hash for rate limiting and does not intentionally store raw IP addresses in match or rate-limit state; providers may keep their own operational logs.
• Advertising, when enabled: Google Mobile Ads/AdMob may process ad-request information, device or advertising identifiers, consent signals and ad interactions under Google's policies. Rewarded ads are optional and Google consent tools may present choices where required.

WHY DATA IS USED AND PROVIDERS
Data supports account creation/recovery, authentication, gameplay, friends and leaderboard features, server-backed entitlements, purchase and rewarded-ad verification, game-integrity checks, abuse prevention, troubleshooting and privacy requests. Depending on features enabled, providers include Cloudflare, the configured EntroNex dice-proof service, Google identity services, Google Play Games Services, Google Play Billing, Google's public STUN service for optional voice-chat setup, and Google Mobile Ads/AdMob with consent tools. Providers apply their own privacy notices. Ludo Paws does not sell personal information.

PURCHASES AND ADS
Google Play shows the live localized price and currency before purchase; this website does not take payments. Source-catalog product IDs and intended fulfillment are described at https://ludopaws.rekixo.com/pricing.html. Products and subscriptions are available only when the corresponding Google Play and server configuration is active. The server verifies purchases before granting Gems or entitlements. When ads are enabled, an opt-in rewarded ad grants its configured reward of five Gems only after server-side verification. Remove Ads suppresses eligible non-rewarded ads but does not prevent optional rewarded ads.

LOCAL STORAGE AND SECURITY
The app uses Android Keystore-backed protection for account credentials and selected sensitive local records where implemented. API requests use HTTPS. Local wallet caches are not trusted as proof of paid entitlements. If local cleanup fails after deletion, clear Ludo Paws app storage on that device.

RETENTION AND ACCOUNT DELETION
In the app, open Profile → menu → Delete account and data and confirm. The account-deletion website at https://ludopaws.rekixo.com/account-deletion.html explains the available deletion routes and provides publisher support contact. If the self-service web form is unavailable, email AI@rekixo.com and request account deletion; ownership verification may be required. For an unlinked guest account, the app can provide a delete-only code for use in the configured guest deletion flow; the code cannot sign in or recover progress and must never be emailed or shared with support.

The server deletion flow removes the account profile, sessions, wallet, cosmetics/reward progress, app-side purchase references, linked social data and leaderboard profile; the app attempts to clear local account caches. Match-proof receipts may be retained for up to 365 days after a match finishes, or 365 days after creation if unfinished. Minimal hashed/pseudonymous deletion and anti-replay markers may remain as needed to prevent account or credential reuse; the current service does not set a fixed expiry for every marker. Google Play order history and independent providers' records are not deleted by Ludo Paws. Offline/local data may remain until app storage is cleared or the app is uninstalled.

CONTACT AND AGE SETTINGS
For privacy questions or account-deletion help, contact AI@rekixo.com. The publisher must ensure the target-audience declaration, age rating, consent treatment, live ad configuration and Google Play Data safety answers match the final released app and its providers."""
