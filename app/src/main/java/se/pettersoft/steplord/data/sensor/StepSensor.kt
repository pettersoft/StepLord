package se.pettersoft.steplord.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject

interface StepSensor {
    val isAvailable: Boolean

    /** Raw step counter values (total steps since boot). Requires ACTIVITY_RECOGNITION on API 29+. */
    fun counterReadings(): Flow<Long>
}

class AndroidStepSensor @Inject constructor(
    @ApplicationContext context: Context,
) : StepSensor {
    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val sensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    override val isAvailable: Boolean get() = sensor != null

    override fun counterReadings(): Flow<Long> {
        val sensor = sensor ?: return emptyFlow()
        return callbackFlow {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    trySend(event.values[0].toLong())
                }

                override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
            }
            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
            awaitClose { sensorManager.unregisterListener(listener) }
        }
    }
}
