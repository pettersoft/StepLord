package se.pettersoft.steplord.data.repository

import java.time.LocalDate

/** Notified whenever the stored step total for a day may have changed. */
fun interface StepsUpdatedListener {
    suspend fun onStepsUpdated(date: LocalDate, steps: Int)
}
