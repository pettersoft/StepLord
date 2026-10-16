package se.pettersoft.steplord.util

import kotlin.math.roundToInt

// Average walking stride length relative to body height
private const val STRIDE_TO_HEIGHT = 0.415

// Typical walking cadence
private const val STEPS_PER_MINUTE = 100

private const val CM_PER_INCH = 2.54
private const val INCHES_PER_FOOT = 12

const val MIN_HEIGHT_CM = 100
const val MAX_HEIGHT_CM = 250

/** Approximate distance walked in meters, from the number of steps and the walker's height. */
fun estimateDistanceMeters(steps: Int, heightCm: Int): Double = steps * heightCm / 100.0 * STRIDE_TO_HEIGHT

/** Approximate time spent walking in minutes, assuming a typical walking pace. */
fun estimateWalkingMinutes(steps: Int): Int = (steps.toDouble() / STEPS_PER_MINUTE).roundToInt()

/** Height in whole feet and inches, e.g. 175 cm -> (5, 9). */
fun cmToFeetAndInches(cm: Int): Pair<Int, Int> {
    val totalInches = (cm / CM_PER_INCH).roundToInt()
    return totalInches / INCHES_PER_FOOT to totalInches % INCHES_PER_FOOT
}

fun feetAndInchesToCm(feet: Int, inches: Int): Int = ((feet * INCHES_PER_FOOT + inches) * CM_PER_INCH).roundToInt()
