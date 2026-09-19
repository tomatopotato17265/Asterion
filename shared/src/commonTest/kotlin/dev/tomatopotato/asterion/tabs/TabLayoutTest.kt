package dev.tomatopotato.asterion.tabs

import kotlin.test.Test
import kotlin.test.assertEquals

class TabLayoutTest {

    @Test
    fun emptyInputFallsBackToDefaultOrder() {
        assertEquals(
            listOf(AppTab.HOME, AppTab.PROJECTS, AppTab.SERVERS, AppTab.INBOX, AppTab.ACCOUNT),
            TabLayout.resolveVisibleTabs(emptyList())
        )
    }

    @Test
    fun validInputPreservesOrder() {
        assertEquals(
            listOf(AppTab.ANALYTICS, AppTab.PROJECTS, AppTab.HOME, AppTab.INBOX, AppTab.ACCOUNT),
            TabLayout.resolveVisibleTabs(listOf("analytics", "projects", "home"))
        )
    }

    @Test
    fun unknownIdIsDropped() {
        assertEquals(
            listOf(AppTab.ANALYTICS, AppTab.PROJECTS, AppTab.HOME, AppTab.INBOX, AppTab.ACCOUNT),
            TabLayout.resolveVisibleTabs(listOf("analytics", "bogus", "projects", "home"))
        )
    }

    @Test
    fun duplicateIdsAreDeduped() {
        assertEquals(
            listOf(AppTab.ANALYTICS, AppTab.PROJECTS, AppTab.HOME, AppTab.INBOX, AppTab.ACCOUNT),
            TabLayout.resolveVisibleTabs(listOf("analytics", "analytics", "projects", "home"))
        )
    }

    @Test
    fun lockedTabIdMixedIntoInputIsDropped() {
        assertEquals(
            listOf(AppTab.ANALYTICS, AppTab.PROJECTS, AppTab.HOME, AppTab.INBOX, AppTab.ACCOUNT),
            TabLayout.resolveVisibleTabs(listOf("analytics", "account", "projects", "home"))
        )
    }

    @Test
    fun shortInputIsPaddedFromDefaults() {
        assertEquals(
            listOf(AppTab.ANALYTICS, AppTab.HOME, AppTab.PROJECTS, AppTab.INBOX, AppTab.ACCOUNT),
            TabLayout.resolveVisibleTabs(listOf("analytics"))
        )
    }

    @Test
    fun resultAlwaysEndsWithTrailingLockedTabs() {
        val result = TabLayout.resolveVisibleTabs(listOf("payouts", "projects", "servers", "home", "analytics"))
        assertEquals(listOf(AppTab.INBOX, AppTab.ACCOUNT), result.takeLast(2))
        assertEquals(5, result.size)
    }
}
