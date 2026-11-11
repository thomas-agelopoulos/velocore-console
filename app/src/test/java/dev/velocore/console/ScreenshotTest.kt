package dev.velocore.console

import com.github.takahirom.roborazzi.captureRoboImage
import dev.velocore.console.bike.BikeSample
import dev.velocore.console.data.RideRecord
import dev.velocore.console.data.RideStore
import dev.velocore.console.data.Workout
import dev.velocore.console.data.Workouts
import dev.velocore.console.ride.RideEngine
import dev.velocore.console.ui.DesignCanvas
import dev.velocore.console.ui.HomeScreen
import dev.velocore.console.ui.RideContent
import dev.velocore.console.ui.SummaryScreen
import dev.velocore.console.ui.WorkoutsScreen
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Draws each screen at the console's 1920 x 1080 so we can look at the real app without the bike.
 * CI records them with `./gradlew recordRoborazziDebug` and attaches them to the run.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [28], qualifiers = "w1920dp-h1080dp-land-mdpi")
class ScreenshotTest {

    private val zone = ZoneId.systemDefault()
    private fun at(day: Int, hour: Int, minute: Int) =
        ZonedDateTime.of(2026, 11, day, hour, minute, 0, 0, zone).toInstant().toEpochMilli()

    private val now = at(11, 7, 42) // Wednesday morning, like the canvas

    /** A rider who sits close to each target, with a little wobble. */
    private fun ride(workout: Workout?, seconds: Int, startedAt: Long): RideEngine {
        val e = RideEngine(workout, startedAt)
        repeat(seconds) { t ->
            val seg = workout?.segments?.getOrNull(e.segmentIndexAt(t))
            val target = seg?.targetWatts
            val base = if (target != null) (target.first + target.last) / 2 else 170
            val watts = (base - 6 + 14 * sin(t * 1.31) + 7 * sin(t * 0.47)).roundToInt()
            e.record(BikeSample(watts, seg?.rpmAim ?: 84, seg?.knob ?: 35, 120 + (watts / 6), 0))
        }
        return e
    }

    private fun history(): List<RideRecord> {
        val monday = ride(null, 45 * 60, at(9, 18, 5)).summary(at(9, 18, 50))
        val tuesday = ride(Workouts.hill, 30 * 60, at(10, 18, 10)).summary(at(10, 18, 40))
        return listOf(monday, tuesday).map {
            RideRecord(it.startedAt, it.workoutId, it.name, it.seconds, it.avgWatts, it.kilojoules, it.avgRpm,
                RideStore.downsample(it.trace, RideStore.SPARK_POINTS))
        }
    }

    private fun shot(name: String, content: @androidx.compose.runtime.Composable () -> Unit) =
        captureRoboImage("build/outputs/roborazzi/$name.png") { DesignCanvas { content() } }

    @Test
    fun home() = shot("1-home") {
        HomeScreen(now, history(), heartConnected = true, onFreeRide = {}, onBrowse = {}, onStart = {}, onNotYet = {})
    }

    @Test
    fun homeFirstDay() = shot("1b-home-no-rides") {
        HomeScreen(now, emptyList(), heartConnected = false, onFreeRide = {}, onBrowse = {}, onStart = {}, onNotYet = {})
    }

    @Test
    fun workouts() = shot("2-workouts") {
        WorkoutsScreen("hill", onBack = {}, onStart = {}, onNotYet = {})
    }

    @Test
    fun riding() {
        val engine = ride(Workouts.hill, 14 * 60 + 12, now)
        shot("3-riding") {
            RideContent(
                workout = Workouts.hill,
                engine = engine,
                progress = engine.progress(),
                sample = BikeSample(196, 72, 53, 148, -3),
                paused = false,
                confirmEnd = false,
                simulated = true,
                onTogglePause = {}, onEnd = {}, onKeepRiding = {}, onConfirmEnd = {},
            )
        }
    }

    @Test
    fun ridingFree() {
        val engine = ride(null, 22 * 60, now)
        shot("3b-riding-free") {
            RideContent(
                workout = null,
                engine = engine,
                progress = engine.progress(),
                sample = BikeSample(182, 86, 38, null, 0),
                paused = false,
                confirmEnd = false,
                simulated = true,
                onTogglePause = {}, onEnd = {}, onKeepRiding = {}, onConfirmEnd = {},
            )
        }
    }

    @Test
    fun ridingEndDialog() {
        val engine = ride(Workouts.tabata, 5 * 60 + 20, now)
        shot("3c-riding-end") {
            RideContent(
                workout = Workouts.tabata,
                engine = engine,
                progress = engine.progress(),
                sample = BikeSample(371, 118, 60, 171, 0),
                paused = false,
                confirmEnd = true,
                simulated = true,
                onTogglePause = {}, onEnd = {}, onKeepRiding = {}, onConfirmEnd = {},
            )
        }
    }

    @Test
    fun summary() {
        val start = at(11, 7, 45)
        val s = ride(Workouts.hill, 30 * 60, start).summary(start + 30 * 60_000L)
        shot("4-summary") {
            SummaryScreen(s, Workouts.hill, history().last(), saved = true, onDone = {}, onRideAgain = {}, onNotYet = {})
        }
    }

    @Test
    fun summaryFree() {
        val start = at(11, 7, 45)
        val s = ride(null, 22 * 60, start).summary(start + 22 * 60_000L)
        shot("4b-summary-free") {
            SummaryScreen(s, null, null, saved = true, onDone = {}, onRideAgain = {}, onNotYet = {})
        }
    }
}
