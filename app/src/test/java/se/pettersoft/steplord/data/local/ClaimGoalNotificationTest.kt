package se.pettersoft.steplord.data.local

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import se.pettersoft.steplord.testing.FakeUserSettingsDao
import java.time.LocalDate

class ClaimGoalNotificationTest {
    private val today = LocalDate.of(2026, 10, 15)

    @Test
    fun claimsOncePerDayWhenGoalReached() = runTest {
        val dao = FakeUserSettingsDao(UserSettings(dailyGoal = 5_000))

        assertFalse(dao.claimGoalNotification(today, 4_999))
        assertTrue(dao.claimGoalNotification(today, 5_000))
        assertFalse(dao.claimGoalNotification(today, 6_000))
        assertEquals(today, dao.get()?.goalNotifiedOn)

        assertTrue(dao.claimGoalNotification(today.plusDays(1), 5_500))
    }

    @Test
    fun usesDefaultsWhenNoSettingsStored() = runTest {
        val dao = FakeUserSettingsDao()

        assertFalse(dao.claimGoalNotification(today, UserSettings.DEFAULT_DAILY_GOAL - 1))
        assertTrue(dao.claimGoalNotification(today, UserSettings.DEFAULT_DAILY_GOAL))
    }

    @Test
    fun neverClaimsWhenDisabled() = runTest {
        val dao = FakeUserSettingsDao(UserSettings(goalNotificationEnabled = false))

        assertFalse(dao.claimGoalNotification(today, 50_000))
    }
}
