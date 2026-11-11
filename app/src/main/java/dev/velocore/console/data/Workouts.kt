package dev.velocore.console.data

import dev.velocore.console.R
import kotlin.math.roundToInt

enum class Category(val label: String) {
    Climbs("Climbs"),
    Intervals("Intervals"),
    Endurance("Endurance"),
    Recovery("Recovery"),
    Tests("Tests"),
}

enum class Kind { Warmup, Work, Recover, Cooldown }

/**
 * One block of a workout. [knob] is the resistance target on the bike's 1 to 100 knob.
 * [rpmLow]..[rpmHigh] is the cadence range; [rpmHigh] null means "rpmLow or more",
 * and both null means "any easy pace".
 */
data class Segment(
    val label: String,
    val seconds: Int,
    val knob: Int,
    val rpmLow: Int?,
    val rpmHigh: Int?,
    val effort: Float,
    val kind: Kind,
) {
    val rpmLabel: String
        get() = when {
            rpmLow == null -> "easy"
            rpmHigh == null -> "$rpmLow+"
            else -> "$rpmLow–$rpmHigh"
        }

    /** Cadence the target watts are worked out at. */
    val rpmAim: Int
        get() = when {
            rpmLow == null -> 85
            rpmHigh == null -> rpmLow + 5
            else -> (rpmLow + rpmHigh) / 2
        }

    val targetWatts: IntRange
        get() {
            val mid = PowerModel.watts(knob, rpmAim)
            val half = (mid * 0.09).roundToInt().coerceAtLeast(10)
            return roundTo5(mid - half)..roundTo5(mid + half)
        }

    fun rpmInRange(rpm: Int): Boolean = when {
        rpmLow == null -> true
        rpmHigh == null -> rpm >= rpmLow
        else -> rpm in rpmLow..rpmHigh
    }

    private fun roundTo5(v: Int) = ((v + 2) / 5) * 5
}

/**
 * Rough watts for a knob position and cadence, until we have real numbers from the bike.
 * Tuned so knob 60 at 70 rpm is about 220 W.
 */
object PowerModel {
    private const val K = 0.052
    fun watts(knob: Int, rpm: Int): Int = (K * knob * rpm).roundToInt()
}

data class PlanRow(val name: String, val time: String, val knob: String, val rpm: String)

data class Workout(
    val id: String,
    val name: String,
    val category: Category,
    val level: String,
    val extra: String,
    val workLabel: String,
    val description: String,
    val cover: Int,
    val segments: List<Segment>,
    val plan: List<PlanRow>,
) {
    val totalSeconds: Int = segments.sumOf { it.seconds }
    val minutes: Int get() = (totalSeconds + 30) / 60
    val workCount: Int get() = segments.count { it.kind == Kind.Work }
    val meta: String get() = "$minutes min · $extra"
}

private fun warm(sec: Int, knob: Int, lo: Int?, hi: Int?, e: Float = 0.35f) =
    Segment("Warm-up", sec, knob, lo, hi, e, Kind.Warmup)

private fun cool(sec: Int, knob: Int, lo: Int?, hi: Int?, e: Float = 0.3f) =
    Segment("Cool-down", sec, knob, lo, hi, e, Kind.Cooldown)

private fun work(label: String, sec: Int, knob: Int, lo: Int?, hi: Int?, e: Float) =
    Segment(label, sec, knob, lo, hi, e, Kind.Work)

private fun easy(label: String, sec: Int, knob: Int, lo: Int?, hi: Int?, e: Float) =
    Segment(label, sec, knob, lo, hi, e, Kind.Recover)

object Workouts {

    val hill = Workout(
        id = "hill", name = "Hill Repeats", category = Category.Climbs, level = "Hard",
        extra = "5 climbs", workLabel = "Climb",
        description = "Five seated climbs with easy spins between. Each climb asks for a little more knob than the last.",
        cover = R.drawable.cover_alpine,
        segments = buildList {
            add(warm(240, 25, 85, 95))
            val knobs = listOf(55, 58, 60, 63, 65)
            knobs.forEachIndexed { i, k ->
                add(work("Climb", 180, k, 65, 75, 0.8f + 0.05f * i))
                if (i < knobs.lastIndex) add(easy("Recover", 120, 30, 85, 95, 0.45f))
            }
            add(cool(180, 20, 80, 90))
        },
        plan = listOf(
            PlanRow("Warm-up", "4:00", "25", "85–95"),
            PlanRow("Climb × 5", "3:00 each", "55 → 65", "65–75"),
            PlanRow("Recover × 4", "2:00 each", "30", "85–95"),
            PlanRow("Cool-down", "3:00", "20", "80–90"),
        ),
    )

    val easySpin = Workout(
        id = "easy", name = "Easy Spin", category = Category.Recovery, level = "Easy",
        extra = "steady", workLabel = "Block",
        description = "Light knob, quick legs. Good the day after a hard ride.",
        cover = R.drawable.cover_forest,
        segments = listOf(
            warm(180, 20, 85, 90, 0.3f),
            Segment("Steady spin", 840, 30, 90, 100, 0.45f, Kind.Work),
            cool(180, 20, 85, 90),
        ),
        plan = listOf(
            PlanRow("Warm-up", "3:00", "20", "85–90"),
            PlanRow("Steady spin", "14:00", "30", "90–100"),
            PlanRow("Cool-down", "3:00", "20", "85–90"),
        ),
    )

    val tabata = Workout(
        id = "tabata", name = "Tabata", category = Category.Intervals, level = "Max",
        extra = "8 rounds", workLabel = "Sprint",
        description = "Eight rounds of 20 seconds all-out and 10 seconds easy, inside a short warm-up and cool-down.",
        cover = R.drawable.cover_velodrome,
        segments = buildList {
            add(warm(240, 25, 85, 95))
            repeat(8) {
                add(work("Sprint", 20, 60, 110, null, 1f))
                add(easy("Rest", 10, 20, null, null, 0.22f))
            }
            add(cool(240, 20, 80, 90))
        },
        plan = listOf(
            PlanRow("Warm-up", "4:00", "25", "85–95"),
            PlanRow("Sprint × 8", "0:20 each", "60", "110+"),
            PlanRow("Rest × 8", "0:10 each", "20", "easy"),
            PlanRow("Cool-down", "4:00", "20", "80–90"),
        ),
    )

    val pyramid = Workout(
        id = "pyramid", name = "Pyramid", category = Category.Intervals, level = "Hard",
        extra = "7 efforts", workLabel = "Effort",
        description = "Efforts of 1, 2, 3, 4, 3, 2 and 1 minutes, each followed by the same time easy.",
        cover = R.drawable.cover_stairs,
        segments = buildList {
            add(warm(180, 25, 85, 95))
            val mins = listOf(1, 2, 3, 4, 3, 2, 1)
            val efforts = listOf(0.8f, 0.8f, 0.82f, 0.85f, 0.82f, 0.8f, 0.8f)
            mins.forEachIndexed { i, m ->
                add(work("Effort", m * 60, 50, 75, 85, efforts[i]))
                if (i < mins.lastIndex) add(easy("Easy", m * 60, 25, 90, 95, 0.35f))
            }
            add(cool(60, 20, 80, 90))
        },
        plan = listOf(
            PlanRow("Warm-up", "3:00", "25", "85–95"),
            PlanRow("Effort × 7", "1 → 4 → 1 min", "50", "75–85"),
            PlanRow("Easy × 6", "same as effort", "25", "90–95"),
            PlanRow("Cool-down", "1:00", "20", "80–90"),
        ),
    )

    val tempo = Workout(
        id = "tempo", name = "Tempo Builder", category = Category.Endurance, level = "Moderate",
        extra = "3 blocks", workLabel = "Block",
        description = "Three long blocks that sit just under hard. Steady breathing, steady knob.",
        cover = R.drawable.cover_desert,
        segments = listOf(
            warm(300, 25, 85, 95),
            work("Tempo", 540, 42, 80, 90, 0.65f),
            easy("Easy", 180, 25, 85, 95, 0.4f),
            work("Tempo", 540, 45, 80, 90, 0.7f),
            easy("Easy", 180, 25, 85, 95, 0.4f),
            work("Tempo", 480, 48, 80, 90, 0.72f),
            cool(180, 20, 80, 90),
        ),
        plan = listOf(
            PlanRow("Warm-up", "5:00", "25", "85–95"),
            PlanRow("Tempo × 3", "8–9 min", "42 → 48", "80–90"),
            PlanRow("Easy × 2", "3:00 each", "25", "85–95"),
            PlanRow("Cool-down", "3:00", "20", "80–90"),
        ),
    )

    val ftp = Workout(
        id = "ftp", name = "FTP Test", category = Category.Tests, level = "Max",
        extra = "ramp", workLabel = "Step",
        description = "The knob goes up a step every minute until you can no longer hold 80 rpm. Sets the targets for every other workout.",
        cover = R.drawable.cover_tunnel,
        segments = buildList {
            add(warm(240, 20, 85, 90, 0.3f))
            repeat(14) { k -> add(work("Step", 60, 30 + 4 * k, 80, null, 0.4f + 0.045f * k)) }
            add(cool(120, 15, null, null, 0.25f))
        },
        plan = listOf(
            PlanRow("Warm-up", "4:00", "20", "85–90"),
            PlanRow("Ramp steps", "1:00 each", "+4 a step", "80+"),
            PlanRow("Cool-down", "2:00", "15", "easy"),
        ),
    )

    val longSteady = Workout(
        id = "long", name = "Long Steady", category = Category.Endurance, level = "Moderate",
        extra = "one block", workLabel = "Block",
        description = "An hour at a pace you could talk through. Pairs well with a show.",
        cover = R.drawable.cover_coast,
        segments = listOf(
            warm(360, 25, 85, 90),
            work("Steady", 2880, 38, 85, 90, 0.55f),
            cool(360, 20, 80, 90),
        ),
        plan = listOf(
            PlanRow("Warm-up", "6:00", "25", "85–90"),
            PlanRow("Steady", "48:00", "38", "85–90"),
            PlanRow("Cool-down", "6:00", "20", "80–90"),
        ),
    )

    val sprintLadder = Workout(
        id = "sprint", name = "Sprint Ladder", category = Category.Intervals, level = "Hard",
        extra = "5 sprints", workLabel = "Sprint",
        description = "Sprints of 10, 15, 20, 25 and 30 seconds with full recovery between.",
        cover = R.drawable.cover_city,
        segments = buildList {
            add(warm(300, 25, 85, 95))
            listOf(10, 15, 20, 25, 30).forEach { s ->
                add(work("Sprint", s, 65, 110, null, 1f))
                add(easy("Recover", 180 - s, 20, null, null, 0.25f))
            }
            add(cool(300, 20, 80, 90))
        },
        plan = listOf(
            PlanRow("Warm-up", "5:00", "25", "85–95"),
            PlanRow("Sprint × 5", "0:10 → 0:30", "65", "110+"),
            PlanRow("Recover × 5", "about 2:45", "20", "easy"),
            PlanRow("Cool-down", "5:00", "20", "80–90"),
        ),
    )

    val rolling = Workout(
        id = "rolling", name = "Rolling Hills", category = Category.Climbs, level = "Moderate",
        extra = "5 rises", workLabel = "Rise",
        description = "Gentle rises and flats. The knob moves often, never far.",
        cover = R.drawable.cover_hills,
        segments = listOf(
            warm(300, 25, 85, 95),
            work("Rise", 240, 40, 70, 85, 0.55f),
            easy("Flat", 180, 32, 85, 90, 0.45f),
            work("Rise", 300, 46, 70, 85, 0.65f),
            easy("Flat", 180, 32, 85, 90, 0.45f),
            work("Rise", 240, 43, 70, 85, 0.6f),
            easy("Flat", 180, 32, 85, 90, 0.45f),
            work("Rise", 360, 52, 70, 85, 0.7f),
            easy("Flat", 180, 32, 85, 90, 0.45f),
            work("Rise", 300, 44, 70, 85, 0.6f),
            cool(240, 20, 80, 90),
        ),
        plan = listOf(
            PlanRow("Warm-up", "5:00", "25", "85–95"),
            PlanRow("Rise × 5", "4–6 min", "40 → 52", "70–85"),
            PlanRow("Flat × 4", "3:00 each", "32", "85–90"),
            PlanRow("Cool-down", "4:00", "20", "80–90"),
        ),
    )

    val all: List<Workout> = listOf(hill, easySpin, tabata, pyramid, tempo, ftp, longSteady, sprintLadder, rolling)

    fun byId(id: String?): Workout? = all.firstOrNull { it.id == id }
}
