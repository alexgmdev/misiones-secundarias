package com.groovycow.misionessecundarias.ui.missions

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class MissionRewardsNoticeTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displaysTheExactProductNoticeAsStaticText() {
        composeTestRule.setContent {
            MaterialTheme {
                MissionRewardsNotice()
            }
        }

        composeTestRule.onNodeWithText(REWARDS_NOTICE_TEXT, useUnmergedTree = true)
            .assertIsDisplayed()
            .assertHasNoClickAction()
    }

    @Test
    fun noticeDoesNotExposePaymentOrNavigationActions() {
        composeTestRule.setContent {
            MaterialTheme {
                MissionRewardsNotice()
            }
        }

        composeTestRule.onAllNodes(hasClickAction(), useUnmergedTree = true).assertCountEquals(0)
    }
}
