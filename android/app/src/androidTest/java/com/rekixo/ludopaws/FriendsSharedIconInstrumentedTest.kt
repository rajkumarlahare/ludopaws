package com.rekixo.ludopaws

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rekixo.ludopaws.core.ui.icons.AppIconView
import com.rekixo.ludopaws.feature.friends.presentation.FriendsIcon
import com.rekixo.ludopaws.feature.friends.presentation.FriendsVisualKit
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FriendsSharedIconInstrumentedTest {
    @Test
    fun commonFriendActionsUseTheSharedIconRegistry() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sharedActions = listOf(
            FriendsIcon.BACK,
            FriendsIcon.PEOPLE,
            FriendsIcon.ADD_FRIEND,
            FriendsIcon.SEARCH,
            FriendsIcon.COPY,
            FriendsIcon.SHARE,
            FriendsIcon.PLAY,
            FriendsIcon.INVITE,
            FriendsIcon.MESSAGE,
            FriendsIcon.CLOCK,
            FriendsIcon.MORE,
        )

        sharedActions.forEach { icon ->
            assertTrue(
                "Expected $icon to use AppIconView",
                FriendsVisualKit.iconView(context, icon) is AppIconView,
            )
        }

        assertFalse(
            "Feature-specific ID card art should remain local",
            FriendsVisualKit.iconView(context, FriendsIcon.ID_CARD) is AppIconView,
        )
        assertFalse(
            "Friend request envelope art should remain feature-specific",
            FriendsVisualKit.iconView(context, FriendsIcon.REQUESTS) is AppIconView,
        )
    }
}
