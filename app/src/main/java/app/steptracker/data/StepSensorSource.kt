package app.steptracker.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.provider.Settings
import app.steptracker.domain.Reading
import java.time.Instant
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** One-shot reads of the hardware cumulative step counter. Not used as a live listener. */
class StepSensorSource(context: Context) {
    private val appContext = context.applicationContext
    private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepCounter: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    fun isSupported(): Boolean = stepCounter != null

    /** Returns the current reading, or null when unsupported, no permission, or on timeout. */
    suspend fun read(): Reading? {
        val sensor = stepCounter ?: return null
        val counter = withTimeoutOrNull(READ_TIMEOUT_MS) {
            suspendCancellableCoroutine<Long> { cont ->
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        sensorManager.unregisterListener(this)
                        if (cont.isActive) cont.resume(event.values[0].toLong())
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
                }
                cont.invokeOnCancellation { sensorManager.unregisterListener(listener) }
                val registered = sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
                if (!registered) {
                    sensorManager.unregisterListener(listener)
                    cont.resume(-1L)
                }
            }
        }
        if (counter == null || counter < 0) return null
        val bootCount = Settings.Global.getInt(appContext.contentResolver, Settings.Global.BOOT_COUNT, -1)
        return Reading(counter = counter, bootCount = bootCount, at = Instant.now())
    }

    private companion object {
        const val READ_TIMEOUT_MS = 5_000L
    }
}
