package dev.velocore.console.bike

import dev.velocore.console.data.PowerModel
import dev.velocore.console.data.Segment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.random.Random

/** One reading from the bike. [heartRate] and [leanDegrees] are null when nothing provides them. */
data class BikeSample(
    val watts: Int,
    val rpm: Int,
    val knob: Int,
    val heartRate: Int?,
    val leanDegrees: Int?,
) {
    companion object {
        val Idle = BikeSample(0, 0, 1, null, null)
    }
}

/**
 * Where ride numbers come from. Today that's [SimulatedBike]; the real source reads the bike's
 * control board once the app is installed on the console.
 */
interface BikeSource {
    val name: String
    val samples: StateFlow<BikeSample>

    /** What the rider is meant to be doing now, so a simulated rider can follow along. */
    var target: () -> Segment?

    fun start(scope: CoroutineScope)
    fun stop()
}

/**
 * A pretend rider for trying the app away from the bike. They pedal, react to the plan a few
 * seconds late and turn the knob one step at a time, so the cues on the riding screen move.
 */
class SimulatedBike(private val random: Random = Random.Default) : BikeSource {
    override val name = "Simulated rider"
    private val state = MutableStateFlow(BikeSample.Idle)
    override val samples: StateFlow<BikeSample> = state.asStateFlow()
    override var target: () -> Segment? = { null }

    private var job: Job? = null
    private var knob = 20.0
    private var rpm = 0.0
    private var heart = 92.0
    private var lastTarget: Segment? = null
    private var reactIn = 0

    override fun start(scope: CoroutineScope) {
        if (job?.isActive == true) return
        job = scope.launch {
            while (isActive) {
                step()
                delay(STEP_MS)
            }
        }
    }

    override fun stop() {
        job?.cancel()
        job = null
        state.value = BikeSample.Idle
        rpm = 0.0
    }

    private fun step() {
        val seg = target()
        if (seg !== lastTarget) {
            lastTarget = seg
            reactIn = (2_500 + random.nextInt(2_500)) / STEP_MS.toInt()
        }
        val wantKnob = (seg?.knob ?: 35).toDouble()
        val wantRpm = (seg?.rpmAim ?: 84).toDouble()

        if (reactIn > 0) {
            reactIn--
        } else if (abs(wantKnob - knob) >= 1 && random.nextInt(3) == 0) {
            knob += sign(wantKnob - knob)
        }
        rpm += (wantRpm - rpm) * 0.12 + (random.nextDouble() - 0.5) * 2.0
        val knobInt = knob.roundToInt().coerceIn(1, 100)
        val rpmInt = rpm.roundToInt().coerceAtLeast(0)
        val noise = (random.nextDouble() - 0.5) * 12
        val watts = (PowerModel.watts(knobInt, rpmInt) + noise).roundToInt().coerceAtLeast(0)
        heart += ((95 + watts * 0.25) - heart) * 0.01
        state.value = BikeSample(
            watts = watts,
            rpm = rpmInt,
            knob = knobInt,
            heartRate = heart.roundToInt(),
            leanDegrees = if (random.nextInt(40) == 0) random.nextInt(-5, 6) else state.value.leanDegrees ?: 0,
        )
    }

    private companion object {
        const val STEP_MS = 250L
    }
}
