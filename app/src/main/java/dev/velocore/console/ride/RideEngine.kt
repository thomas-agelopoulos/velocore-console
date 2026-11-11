package dev.velocore.console.ride

import dev.velocore.console.bike.BikeSample
import dev.velocore.console.data.Kind
import dev.velocore.console.data.Segment
import dev.velocore.console.data.Workout
import kotlin.math.abs
import kotlin.math.roundToInt

data class RideSecond(val watts: Int, val rpm: Int, val knob: Int, val heartRate: Int?)

enum class Effort { Under, InZone, Over }
enum class Cue { Right, Left, Hold }

/** Where the rider is in the plan right now. */
data class RideProgress(
    val elapsed: Int,
    val total: Int?,
    val segmentIndex: Int,
    val segment: Segment?,
    val segmentRemaining: Int,
    val workNumber: Int,
    val workCount: Int,
    val next: Segment?,
    val avgWatts: Int,
    val kilojoules: Int,
    val miles: Double,
)

data class SegmentResult(
    val name: String,
    val target: IntRange?,
    val avgWatts: Int,
    val avgRpm: Int,
    val onTargetPct: Int?,
)

data class RideSummary(
    val workoutId: String?,
    val name: String,
    val workLabel: String,
    val startedAt: Long,
    val endedAt: Long,
    val seconds: Int,
    val kilojoules: Int,
    val avgWatts: Int,
    val avgRpm: Int,
    val avgHeartRate: Int?,
    val maxHeartRate: Int?,
    val calories: Int,
    val miles: Double,
    val workDone: Int,
    val workTotal: Int,
    val results: List<SegmentResult>,
    val onTargetPct: Int?,
    val trace: List<Int>,
)

/**
 * Keeps the books for one ride: one entry per second of riding (pauses don't count), plus
 * which segment of the plan that second fell in. Plain Kotlin so it can be unit tested.
 */
class RideEngine(val workout: Workout?, val startedAt: Long) {
    private val log = ArrayList<RideSecond>()
    private var joules = 0.0
    private var meters = 0.0

    private val starts: IntArray = workout?.segments
        ?.runningFold(0) { acc, s -> acc + s.seconds }
        ?.toIntArray()
        ?: IntArray(0)

    val elapsed: Int get() = log.size
    val seconds: List<RideSecond> get() = log
    val isComplete: Boolean get() = workout != null && elapsed >= workout.totalSeconds

    fun record(sample: BikeSample) {
        log += RideSecond(sample.watts, sample.rpm, sample.knob, sample.heartRate)
        joules += sample.watts
        meters += Physics.speedMps(sample.watts)
    }

    /** Index of the segment that second [t] falls in, or -1 on a free ride. */
    fun segmentIndexAt(t: Int): Int {
        val segs = workout?.segments ?: return -1
        for (i in segs.indices) if (t < starts[i + 1]) return i
        return segs.lastIndex
    }

    fun segmentStart(i: Int): Int = starts[i]
    fun segmentEnd(i: Int): Int = starts[i + 1]

    fun progress(): RideProgress {
        val segs = workout?.segments
        val i = segmentIndexAt(elapsed)
        val seg = if (segs != null && i >= 0) segs[i] else null
        val workNumber = if (segs != null && i >= 0) {
            segs.subList(0, i + 1).count { it.kind == Kind.Work }
        } else {
            0
        }
        return RideProgress(
            elapsed = elapsed,
            total = workout?.totalSeconds,
            segmentIndex = i,
            segment = seg,
            segmentRemaining = if (i >= 0) (segmentEnd(i) - elapsed).coerceAtLeast(0) else 0,
            workNumber = workNumber,
            workCount = workout?.workCount ?: 0,
            next = if (segs != null && i >= 0 && i < segs.lastIndex) segs[i + 1] else null,
            avgWatts = avgWatts(),
            kilojoules = (joules / 1000).roundToInt(),
            miles = meters / METERS_PER_MILE,
        )
    }

    private fun avgWatts(): Int = if (log.isEmpty()) 0 else (joules / log.size).roundToInt()

    fun summary(endedAt: Long): RideSummary {
        val pedaling = log.filter { it.rpm > 0 }
        val hearts = log.mapNotNull { it.heartRate }
        val results = ArrayList<SegmentResult>()
        var onSeconds = 0
        var workSeconds = 0
        var workDone = 0
        val w = workout
        if (w != null) {
            var n = 0
            w.segments.forEachIndexed { i, s ->
                if (s.kind != Kind.Work) return@forEachIndexed
                n++
                val from = segmentStart(i)
                if (from >= elapsed) return@forEachIndexed
                val to = minOf(segmentEnd(i), elapsed)
                if (to >= segmentEnd(i)) workDone++
                val slice = log.subList(from, to)
                val target = s.targetWatts
                val on = slice.count { it.watts in target }
                onSeconds += on
                workSeconds += slice.size
                results += SegmentResult(
                    name = "${w.workLabel} $n",
                    target = target,
                    avgWatts = slice.averageOf { it.watts },
                    avgRpm = slice.averageOf { it.rpm },
                    onTargetPct = if (slice.isEmpty()) 0 else (on * 100.0 / slice.size).roundToInt(),
                )
            }
        } else {
            log.chunked(SPLIT_SECONDS).forEachIndexed { k, slice ->
                val a = k * SPLIT_SECONDS / 60
                val b = (k * SPLIT_SECONDS + slice.size) / 60
                results += SegmentResult(
                    name = "$a–$b min",
                    target = null,
                    avgWatts = slice.averageOf { it.watts },
                    avgRpm = slice.averageOf { it.rpm },
                    onTargetPct = null,
                )
            }
        }
        val kj = (joules / 1000).roundToInt()
        return RideSummary(
            workoutId = workout?.id,
            name = workout?.name ?: "Free ride",
            workLabel = workout?.workLabel ?: "Split",
            startedAt = startedAt,
            endedAt = endedAt,
            seconds = elapsed,
            kilojoules = kj,
            avgWatts = avgWatts(),
            avgRpm = pedaling.averageOf { it.rpm },
            avgHeartRate = if (hearts.isEmpty()) null else hearts.average().roundToInt(),
            maxHeartRate = hearts.maxOrNull(),
            // Legs turn roughly a quarter of food energy into work, so kcal burned ≈ kJ of work.
            calories = kj,
            miles = meters / METERS_PER_MILE,
            workDone = workDone,
            workTotal = workout?.workCount ?: 0,
            results = results,
            onTargetPct = if (workSeconds == 0) null else (onSeconds * 100.0 / workSeconds).roundToInt(),
            trace = log.map { it.watts },
        )
    }

    companion object {
        const val SPLIT_SECONDS = 300
        private const val METERS_PER_MILE = 1609.344

        fun effort(watts: Int, target: IntRange): Effort = when {
            watts < target.first -> Effort.Under
            watts > target.last -> Effort.Over
            else -> Effort.InZone
        }

        /** Which way to turn the knob, and by how much. Within 2 steps counts as on target. */
        fun cue(knob: Int, target: Int): Pair<Cue, Int> {
            val diff = target - knob
            return when {
                abs(diff) <= 2 -> Cue.Hold to 0
                diff > 0 -> Cue.Right to diff
                else -> Cue.Left to -diff
            }
        }
    }
}

private inline fun <T> List<T>.averageOf(f: (T) -> Int): Int =
    if (isEmpty()) 0 else (sumOf { f(it).toLong() }.toDouble() / size).roundToInt()

/** Virtual road speed from power, the way smart trainers do it (flat road, 85 kg rider and bike). */
object Physics {
    private const val AERO = 0.5 * 1.2 * 0.4 // half air density x CdA
    private const val ROLL = 0.004 * 85 * 9.81 // Crr x mass x g

    fun speedMps(watts: Int): Double {
        if (watts <= 0) return 0.0
        var lo = 0.0
        var hi = 30.0
        repeat(40) {
            val v = (lo + hi) / 2
            if (AERO * v * v * v + ROLL * v < watts) lo = v else hi = v
        }
        return (lo + hi) / 2
    }
}
