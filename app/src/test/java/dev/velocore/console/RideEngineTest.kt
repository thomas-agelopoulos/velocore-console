package dev.velocore.console

import dev.velocore.console.bike.BikeSample
import dev.velocore.console.data.Kind
import dev.velocore.console.data.RideStore
import dev.velocore.console.data.Workouts
import dev.velocore.console.ride.Cue
import dev.velocore.console.ride.Effort
import dev.velocore.console.ride.Physics
import dev.velocore.console.ride.RideEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class WorkoutsTest {
    @Test
    fun lengthsMatchTheCards() {
        val expected = mapOf(
            "hill" to 30, "easy" to 20, "tabata" to 12, "pyramid" to 35, "tempo" to 40,
            "ftp" to 20, "long" to 60, "sprint" to 25, "rolling" to 45,
        )
        Workouts.all.forEach { w -> assertEquals(w.name, expected.getValue(w.id) * 60, w.totalSeconds) }
    }

    @Test
    fun workCountsMatchTheCards() {
        assertEquals(5, Workouts.hill.workCount)
        assertEquals(8, Workouts.tabata.workCount)
        assertEquals(7, Workouts.pyramid.workCount)
        assertEquals(5, Workouts.sprintLadder.workCount)
        assertEquals(5, Workouts.rolling.workCount)
    }

    @Test
    fun thirdClimbTargetsAbout200to240() {
        val climb3 = Workouts.hill.segments.filter { it.kind == Kind.Work }[2]
        assertEquals(60, climb3.knob)
        val t = climb3.targetWatts
        assertTrue("low ${t.first}", t.first in 190..210)
        assertTrue("high ${t.last}", t.last in 230..250)
    }

    @Test
    fun openEndedCadence() {
        val sprint = Workouts.tabata.segments.first { it.kind == Kind.Work }
        assertEquals("110+", sprint.rpmLabel)
        assertTrue(sprint.rpmInRange(125))
        assertFalse(sprint.rpmInRange(100))
    }
}

class RideEngineTest {
    private fun ride(engine: RideEngine, seconds: Int, watts: Int, rpm: Int = 80, knob: Int = 40) {
        repeat(seconds) { engine.record(BikeSample(watts, rpm, knob, 140, 0)) }
    }

    @Test
    fun findsTheRightSegment() {
        val e = RideEngine(Workouts.hill, 0)
        assertEquals(0, e.segmentIndexAt(0))
        assertEquals(0, e.segmentIndexAt(239))
        assertEquals(1, e.segmentIndexAt(240))
        assertEquals(Workouts.hill.segments.lastIndex, e.segmentIndexAt(10_000))
    }

    @Test
    fun progressCountsClimbs() {
        val e = RideEngine(Workouts.hill, 0)
        ride(e, 14 * 60 + 12, 200) // 14:12 in, like the canvas
        val p = e.progress()
        assertEquals("Climb", p.segment?.label)
        assertEquals(3, p.workNumber)
        assertEquals(5, p.workCount)
        assertEquals(168, p.segmentRemaining) // 2:48 left
        assertEquals("Recover", p.next?.label)
    }

    @Test
    fun energyAndAverages() {
        val e = RideEngine(null, 0)
        ride(e, 600, 200)
        val p = e.progress()
        assertEquals(200, p.avgWatts)
        assertEquals(120, p.kilojoules)
        assertTrue(p.miles > 2.0 && p.miles < 4.0)
        assertNull(p.segment)
        assertFalse(e.isComplete)
    }

    @Test
    fun summaryScoresTimeOnTarget() {
        val w = Workouts.hill
        val e = RideEngine(w, 0)
        val climb1 = w.segments[1].targetWatts
        ride(e, 240, 120) // warm-up
        ride(e, 90, climb1.first + 5) // half the climb on target
        ride(e, 90, climb1.first - 30) // half under
        val s = e.summary(1_000)
        assertEquals(1, s.results.size)
        assertEquals(50, s.results[0].onTargetPct)
        assertEquals(50, s.onTargetPct)
        assertEquals(1, s.workDone)
        assertEquals(5, s.workTotal)
        assertEquals(420, s.trace.size)
        assertEquals(140, s.avgHeartRate)
    }

    @Test
    fun freeRideSplitsEveryFiveMinutes() {
        val e = RideEngine(null, 0)
        ride(e, 720, 150)
        val s = e.summary(0)
        assertEquals(listOf("0–5 min", "5–10 min", "10–12 min"), s.results.map { it.name })
        assertNull(s.onTargetPct)
    }

    @Test
    fun cuesAndEffort() {
        assertEquals(Cue.Right to 7, RideEngine.cue(53, 60))
        assertEquals(Cue.Left to 8, RideEngine.cue(68, 60))
        assertEquals(Cue.Hold to 0, RideEngine.cue(61, 60))
        assertEquals(Effort.Under, RideEngine.effort(196, 200..240))
        assertEquals(Effort.InZone, RideEngine.effort(218, 200..240))
        assertEquals(Effort.Over, RideEngine.effort(262, 200..240))
    }

    @Test
    fun speedGrowsWithPower() {
        assertEquals(0.0, Physics.speedMps(0), 0.0)
        assertTrue(Physics.speedMps(100) < Physics.speedMps(200))
        assertTrue(Physics.speedMps(176) in 8.0..9.0)
    }
}

class RideStoreTest {
    @Test
    fun downsampleKeepsShape() {
        val values = (0 until 1800).map { it / 10 }
        val d = RideStore.downsample(values, 88)
        assertEquals(88, d.size)
        assertTrue(d.first() < d.last())
    }

    @Test
    fun weekStartsMonday() {
        // Wednesday 2026-11-11
        val today = LocalDate.of(2026, 11, 11)
        val monday = LocalDate.of(2026, 11, 9).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val lastSunday = LocalDate.of(2026, 11, 8).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val rides = listOf(
            record(monday + 3_600_000, 45 * 60),
            record(lastSunday + 3_600_000, 30 * 60),
        )
        val week = RideStore.weekMinutes(rides, today, ZoneOffset.UTC)
        assertEquals(45, week[0])
        assertEquals(45, week.sum())
    }

    private fun record(at: Long, seconds: Int) =
        dev.velocore.console.data.RideRecord(at, null, "Free ride", seconds, 150, 100, 80, emptyList())
}
