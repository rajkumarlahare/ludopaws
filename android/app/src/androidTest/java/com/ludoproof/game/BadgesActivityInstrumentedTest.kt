package com.ludoproof.game

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ludoproof.game.feature.profile.presentation.BadgesActivity
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BadgesActivityInstrumentedTest {
    @Test
    fun badgeCollectionListsEveryKnownBadge() {
        ActivityScenario.launch(BadgesActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val labels = mutableSetOf<String>()
                collectText(activity.window.decorView, labels)
                listOf(
                    "First Win",
                    "Strategist",
                    "Unstoppable",
                    "Friendship",
                    "Social Gamer",
                    "Ludo Novice",
                ).forEach { badge ->
                    assertTrue("Badge page is missing $badge", badge in labels)
                }
                assertTrue("Badge page should show the collection heading", "ALL BADGES" in labels)
            }
        }
    }

    private fun collectText(view: View, labels: MutableSet<String>) {
        if (view is TextView) labels += view.text?.toString().orEmpty()
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                collectText(view.getChildAt(index), labels)
            }
        }
    }
}
