package se.pettersoft.steplord.data.history

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.fitness.FitnessLocal
import com.google.android.gms.fitness.LocalRecordingClient
import com.google.android.gms.fitness.data.LocalDataType
import com.google.android.gms.fitness.data.LocalField
import com.google.android.gms.fitness.request.LocalDataReadRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Step history recorded by the system in the background, independent of whether our app is running.
 */
interface StepHistory {
    val isAvailable: Boolean

    /** Starts (or renews) background recording. Safe to call repeatedly; throws SecurityException without ACTIVITY_RECOGNITION. */
    suspend fun subscribe()

    /** Total steps per local day in [from]..[to], for days that have recorded data. */
    suspend fun dailyTotals(from: LocalDate, to: LocalDate): Map<LocalDate, Int>
}

/**
 * Backed by the Recording API on mobile: Google Play services keeps the step sensor registered
 * in a battery-efficient way and keeps up to 10 days of data on the device.
 */
class RecordingApiStepHistory @Inject constructor(
    @ApplicationContext private val context: Context,
) : StepHistory {
    private val client by lazy { FitnessLocal.getLocalRecordingClient(context) }

    override val isAvailable: Boolean
        get() = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
            context,
            LocalRecordingClient.LOCAL_RECORDING_CLIENT_STEPS_MIN_VERSION_CODE,
        ) == ConnectionResult.SUCCESS

    override suspend fun subscribe() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            throw SecurityException("ACTIVITY_RECOGNITION is required to record steps")
        }
        client.subscribe(LocalDataType.TYPE_STEP_COUNT_DELTA).await()
    }

    override suspend fun dailyTotals(from: LocalDate, to: LocalDate): Map<LocalDate, Int> {
        val zone = ZoneId.systemDefault()
        val start = from.atStartOfDay(zone).toEpochSecond()
        val end = minOf(to.plusDays(1).atStartOfDay(zone).toEpochSecond(), Instant.now().epochSecond)
        // Hourly buckets grouped by local date stay correct across daylight saving changes,
        // which daily buckets (fixed 24h) would not
        val request = LocalDataReadRequest.Builder()
            .aggregate(LocalDataType.TYPE_STEP_COUNT_DELTA)
            .bucketByTime(1, TimeUnit.HOURS)
            .setTimeRange(start, end, TimeUnit.SECONDS)
            .build()
        val response = client.readData(request).await()

        return response.buckets
            .groupBy { Instant.ofEpochSecond(it.getStartTime(TimeUnit.SECONDS)).atZone(zone).toLocalDate() }
            .mapValues { (_, buckets) ->
                buckets.sumOf { bucket ->
                    bucket.dataSets.sumOf { dataSet ->
                        dataSet.dataPoints.sumOf { it.getValue(LocalField.FIELD_STEPS).asInt() }
                    }
                }
            }
            .filterValues { it > 0 }
    }
}
