package se.pettersoft.steplord.util

import org.junit.Assert.assertEquals
import org.junit.Test

class WalkingEstimatesTest {
    @Test
    fun distanceUsesStrideFromHeight() {
        // 175 cm * 0.415 = 0.726 m per step
        assertEquals(7_262.5, estimateDistanceMeters(10_000, 175), 0.01)
        assertEquals(0.0, estimateDistanceMeters(0, 175), 0.0)
        // Taller people take longer steps
        assertEquals(8_300.0, estimateDistanceMeters(10_000, 200), 0.01)
    }

    @Test
    fun walkingTimeAssumesHundredStepsPerMinute() {
        assertEquals(76, estimateWalkingMinutes(7_559))
        assertEquals(0, estimateWalkingMinutes(40))
        assertEquals(1, estimateWalkingMinutes(50))
    }

    @Test
    fun convertsBetweenCentimetersAndFeetAndInches() {
        assertEquals(5 to 9, cmToFeetAndInches(175))
        assertEquals(6 to 0, cmToFeetAndInches(183))
        assertEquals(175, feetAndInchesToCm(5, 9))
        // Round trip within an inch
        for (cm in 150..200) {
            val (feet, inches) = cmToFeetAndInches(cm)
            assert(kotlin.math.abs(feetAndInchesToCm(feet, inches) - cm) <= 2) { "cm=$cm" }
        }
    }
}
