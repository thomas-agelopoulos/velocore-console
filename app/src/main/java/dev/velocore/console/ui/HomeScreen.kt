package dev.velocore.console.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import dev.velocore.console.R
import dev.velocore.console.data.RideRecord
import dev.velocore.console.data.RideStore
import dev.velocore.console.data.Workout
import dev.velocore.console.data.Workouts
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime

private const val WEEK_GOAL_MINUTES = 150
private const val RIDER_NAME = "Thomas"

@Composable
fun HomeScreen(
    rides: List<RideRecord>,
    heartConnected: Boolean,
    onFreeRide: () -> Unit,
    onBrowse: (selectedId: String) -> Unit,
    onStart: (Workout) -> Unit,
    onNotYet: (String) -> Unit,
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(10_000)
            now = System.currentTimeMillis()
        }
    }
    val today = LocalDate.now()
    val hour = LocalDateTime.now().hour

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 64.dp, end = 64.dp, top = 44.dp, bottom = 52.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            T(Format.longDate(today), ts(22, 500, Palette.Text2))
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(22.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Wifi, 26)
                if (heartConnected) Icon(Icons.Heart, 26, Palette.Red)
            }
            Spacer(Modifier.width(28.dp))
            T(Format.now(now), ts(30, 600, tracking = -0.5f, tabular = true))
        }

        T("${Format.greeting(hour)}, $RIDER_NAME", ts(76, 700, tracking = -2.4f, lineHeight = 1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Hero(onFreeRide)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    SuggestedCard(Workouts.hill, Modifier.weight(1f).fillMaxHeight()) { onBrowse(Workouts.hill.id) }
                    WatchAndRideCard(Modifier.weight(1f).fillMaxHeight()) {
                        onNotYet("Watch and ride is coming in a later build.")
                    }
                    QuickStartCard(Modifier.weight(1f).fillMaxHeight(), onStart)
                }
            }

            Column(
                modifier = Modifier
                    .width(600.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                WeekCard(RideStore.weekMinutes(rides, today), today.dayOfWeek.value - 1)
                LastRideCard(rides.lastOrNull(), today, Modifier.weight(1f).fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    SecondaryPill("History", Modifier.weight(1f), icon = Icons.History) {
                        onNotYet("Ride history is coming in a later build.")
                    }
                    SecondaryPill("Settings", Modifier.weight(1f), icon = Icons.Gear) {
                        onNotYet("Settings are coming in a later build.")
                    }
                }
            }
        }
    }
}

@Composable
private fun Hero(onStart: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(470.dp)
            .pressable(onClick = onStart)
            .card(Palette.Surface1, 40),
    ) {
        Cover(R.drawable.cover_road, verticalBias = -0.2f)
        BottomScrim(0.75f, 0.82f)
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 52.dp, end = 52.dp, bottom = 48.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                T("Free ride", ts(22, 600, Palette.text(0.78f)))
                T("Just ride.", ts(104, 700, tracking = -4f, lineHeight = 0.92f))
                T("No plan. Every pedal stroke counted.", ts(26, 400, Palette.text(0.82f)))
            }
            Row(
                modifier = Modifier
                    .height(92.dp)
                    .clip(RoundedCornerShape(46.dp))
                    .background(Palette.Text)
                    .padding(start = 36.dp, end = 44.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Play, 30, Palette.Ink)
                T("Start", ts(28, 600, Palette.Ink))
            }
        }
    }
}

@Composable
private fun SuggestedCard(workout: Workout, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.pressable(onClick = onClick).card(Palette.Surface1, 32)) {
        Cover(workout.cover)
        BottomScrim(0.7f, 0.85f)
        GlassChip("Suggested", modifier = Modifier.align(Alignment.TopStart).padding(24.dp))
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 30.dp, end = 30.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            T(workout.name, ts(40, 700, tracking = -1.2f, lineHeight = 1.05f))
            T("${workout.minutes} min · ${workout.extra} · ${workout.level}", ts(20, 400, Palette.text(0.8f)))
        }
    }
}

@Composable
private fun WatchAndRideCard(modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .pressable(onClick = onClick)
            .card(Palette.Surface1, 32)
            .padding(30.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Palette.Surface2),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Tv, 30) }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            T("Watch and ride", ts(34, 700, tracking = -1f))
            T("Netflix, YouTube or Disney+, with your watts in a corner.", ts(20, 400, Palette.Text2, lineHeight = 1.4f))
        }
    }
}

@Composable
private fun QuickStartCard(modifier: Modifier, onStart: (Workout) -> Unit) {
    Column(
        modifier = modifier
            .card(Palette.Surface1, 32)
            .padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        T("Quick start", ts(22, 600), Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp))
        listOf(
            "Easy spin" to Workouts.easySpin,
            "Tabata" to Workouts.tabata,
            "FTP test" to Workouts.ftp,
        ).forEach { (label, w) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(62.dp)
                    .pressable { onStart(w) }
                    .clip(RoundedCornerShape(20.dp))
                    .background(Palette.Surface2)
                    .padding(horizontal = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                T(label, ts(21, 600), Modifier.weight(1f))
                T("${w.minutes} min", ts(21, 500, Palette.Text2))
            }
        }
    }
}

@Composable
private fun WeekCard(minutes: IntArray, todayIndex: Int) {
    val total = minutes.sum()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .card(Palette.Surface1, 32)
            .padding(horizontal = 36.dp, vertical = 30.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            T("This week", ts(24, 600), Modifier.weight(1f))
            T("Goal ${Format.goal(WEEK_GOAL_MINUTES)}", ts(19, 400, Palette.Text2))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(34.dp)) {
            Box(Modifier.size(188.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 22.dp.toPx()
                    val r = 80.dp.toPx()
                    val topLeft = Offset(center.x - r, center.y - r)
                    val arcSize = Size(r * 2, r * 2)
                    drawArc(Palette.CyanTrack, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
                    val sweep = 360f * (total.toFloat() / WEEK_GOAL_MINUTES).coerceIn(0f, 1f)
                    if (sweep > 0f) {
                        drawArc(Palette.Cyan, -90f, sweep, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    T(Format.hoursMinutes(total), ts(44, 700, tracking = -1.5f, lineHeight = 1f, tabular = true))
                    T("of ${Format.hoursMinutes(WEEK_GOAL_MINUTES)}", ts(17, 400, Palette.Text2))
                }
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(160.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEachIndexed { i, day ->
                    val m = minutes[i]
                    val isToday = i == todayIndex
                    val done = m > 0
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Bottom),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        val shape = RoundedCornerShape(8.dp)
                        val h = if (done) (m / 60f * 120f).coerceIn(12f, 120f) else 12f
                        var bar = Modifier
                            .fillMaxWidth()
                            .height(h.dp)
                            .clip(shape)
                        bar = when {
                            done -> bar.background(Palette.Cyan)
                            isToday -> bar.border(2.dp, Palette.Cyan, shape)
                            else -> bar.background(Palette.Surface2)
                        }
                        Box(bar)
                        T(day, ts(16, if (isToday) 700 else 500, if (isToday) Palette.Cyan else Palette.Text2))
                    }
                }
            }
        }
    }
}

@Composable
private fun LastRideCard(ride: RideRecord?, today: LocalDate, modifier: Modifier) {
    Column(
        modifier = modifier
            .card(Palette.Surface1, 32)
            .padding(horizontal = 36.dp, vertical = 30.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            T("Last ride", ts(24, 600), Modifier.weight(1f))
            if (ride != null) T(Format.relative(ride.startedAt, today), ts(19, 400, Palette.Text2))
        }
        if (ride == null) {
            T("No rides yet", ts(40, 700, tracking = -1.2f, lineHeight = 1f))
            T("Your first ride will show up here.", ts(20, 400, Palette.Text2))
            return@Column
        }
        T(ride.name, ts(40, 700, tracking = -1.2f, lineHeight = 1f), maxLines = 1)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stat(Format.clock(ride.seconds), "Time", Palette.Text, Modifier.weight(1f))
            Stat("${ride.avgWatts}", "Avg watts", Palette.Orange, Modifier.weight(1f))
            Stat("${ride.kilojoules}", "Output kJ", Palette.Text, Modifier.weight(1f))
            Stat("${ride.avgRpm}", "Avg rpm", Palette.Text, Modifier.weight(1f))
        }
        Spacer(Modifier.weight(1f))
        Sparkline(ride.spark, Modifier.fillMaxWidth().height(84.dp))
    }
}

@Composable
private fun Stat(value: String, label: String, color: Color, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        T(value, ts(38, 700, color, tracking = -1.2f, lineHeight = 1f, tabular = true), maxLines = 1)
        T(label, ts(17, 400, Palette.Text2))
    }
}

@Composable
private fun Sparkline(values: List<Int>, modifier: Modifier) {
    if (values.size < 2) return
    Canvas(modifier) {
        val max = (values.maxOrNull() ?: 1).coerceAtLeast(1) * 1.1f
        val inset = 2.dp.toPx()
        val w = size.width - inset * 2
        val h = size.height
        val line = Path()
        val area = Path()
        area.moveTo(inset, h)
        values.forEachIndexed { i, v ->
            val x = inset + w * i / (values.size - 1)
            val y = h - 4.dp.toPx() - (v / max) * (h - 8.dp.toPx())
            if (i == 0) line.moveTo(x, y) else line.lineTo(x, y)
            area.lineTo(x, y)
        }
        area.lineTo(inset + w, h)
        area.close()
        drawPath(area, Palette.OrangeArea)
        drawPath(line, Palette.Orange, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
