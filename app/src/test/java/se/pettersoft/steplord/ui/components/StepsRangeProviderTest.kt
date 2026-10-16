package se.pettersoft.steplord.ui.components

import com.patrykandpatrick.vico.compose.common.data.MutableExtraStore
import org.junit.Assert.assertEquals
import org.junit.Test

class StepsRangeProviderTest {
    private fun maxY(dataMax: Double) = StepsRangeProvider.getMaxY(0.0, dataMax, MutableExtraStore())

    @Test
    fun emptyWeekStillHasWholeNumberScale() {
        assertEquals(1_000.0, maxY(0.0), 0.0)
        assertEquals(1_000.0, maxY(250.0), 0.0)
    }

    @Test
    fun roundsUpToTwoSignificantDigits() {
        assertEquals(14_000.0, maxY(13_456.0), 0.0)
        assertEquals(8_200.0, maxY(8_123.0), 0.0)
        assertEquals(10_000.0, maxY(10_000.0), 0.0)
    }

    @Test
    fun alwaysStartsAtZero() {
        assertEquals(0.0, StepsRangeProvider.getMinY(500.0, 9_000.0, MutableExtraStore()), 0.0)
    }
}
