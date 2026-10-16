package se.pettersoft.steplord.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

private const val MAX_CHECK_INTERVAL_MS = 60_000L

/** The current local date as a flow, so screens follow along when the day changes. */
fun interface CurrentDate {
    fun observe(): Flow<LocalDate>
}

class SystemCurrentDate @Inject constructor() : CurrentDate {
    /**
     * Emits today's date and again after midnight. Checks at least once a minute, because
     * coroutine delays don't advance while the device sleeps, and the user can change the
     * time zone. Collectors should stop collecting when not visible; restarting emits right away.
     */
    override fun observe(): Flow<LocalDate> = flow {
        while (true) {
            val now = LocalDateTime.now()
            emit(now.toLocalDate())
            val untilMidnight = Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay()).toMillis()
            delay(minOf(untilMidnight + 1, MAX_CHECK_INTERVAL_MS))
        }
    }.distinctUntilChanged()
}
