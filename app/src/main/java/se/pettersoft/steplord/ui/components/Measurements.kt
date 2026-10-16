package se.pettersoft.steplord.ui.components

import android.icu.text.MeasureFormat
import android.icu.text.NumberFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import android.icu.util.ULocale
import se.pettersoft.steplord.data.local.UnitSystem
import se.pettersoft.steplord.util.cmToFeetAndInches
import java.util.Locale
import kotlin.math.roundToInt

// Localized unit formatting (e.g. "5.7 km", "5,7 km", "3.5 mi", "1 hr, 16 min") from the platform,
// so unit names and number formats don't need translating

private const val METERS_PER_MILE = 1_609.344

private fun measureFormat(locale: Locale, fractionDigits: Int = 0): MeasureFormat {
    val numberFormat = NumberFormat.getInstance(locale).apply {
        minimumFractionDigits = fractionDigits
        maximumFractionDigits = fractionDigits
    }
    return MeasureFormat.getInstance(ULocale.forLocale(locale), MeasureFormat.FormatWidth.SHORT, numberFormat)
}

/** Meters below 1 km, otherwise km with one decimal; miles with one decimal for imperial. */
fun formatDistance(meters: Double, unitSystem: UnitSystem, locale: Locale = Locale.getDefault()): String =
    when (unitSystem) {
        UnitSystem.METRIC ->
            if (meters < 1_000) {
                measureFormat(locale).format(Measure((meters / 10).roundToInt() * 10, MeasureUnit.METER))
            } else {
                measureFormat(locale, fractionDigits = 1).format(Measure(meters / 1_000, MeasureUnit.KILOMETER))
            }
        UnitSystem.IMPERIAL ->
            measureFormat(locale, fractionDigits = 1).format(Measure(meters / METERS_PER_MILE, MeasureUnit.MILE))
    }

/** E.g. "16 min", "2 hr" or "1 hr, 16 min", in the locale's format. */
fun formatDuration(minutes: Int, locale: Locale = Locale.getDefault()): String {
    val hours = minutes / 60
    val rest = minutes % 60
    val format = measureFormat(locale)
    return when {
        hours == 0 -> format.format(Measure(rest, MeasureUnit.MINUTE))
        rest == 0 -> format.format(Measure(hours, MeasureUnit.HOUR))
        else -> format.formatMeasures(Measure(hours, MeasureUnit.HOUR), Measure(rest, MeasureUnit.MINUTE))
    }
}

/** E.g. "175 cm", or "5 ft, 9 in" for imperial. */
fun formatHeight(heightCm: Int, unitSystem: UnitSystem, locale: Locale = Locale.getDefault()): String {
    val format = measureFormat(locale)
    return when (unitSystem) {
        UnitSystem.METRIC -> format.format(Measure(heightCm, MeasureUnit.CENTIMETER))
        UnitSystem.IMPERIAL -> {
            val (feet, inches) = cmToFeetAndInches(heightCm)
            format.formatMeasures(Measure(feet, MeasureUnit.FOOT), Measure(inches, MeasureUnit.INCH))
        }
    }
}
