package dev.velocore.console.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.velocore.console.R
import dev.velocore.console.bike.BikeSample
import dev.velocore.console.bike.BikeSource
import dev.velocore.console.bike.SimulatedBike
import dev.velocore.console.data.Kind
import dev.velocore.console.data.Segment
import dev.velocore.console.data.Workout
import dev.velocore.console.ride.Cue
import dev.velocore.console.ride.Effort
import dev.velocore.console.ride.RideEngine
import dev.velocore.console.ride.RideProgress
import dev.velocore.console.ride.RideSummary
import kotlinx.coroutines.delay
import kotlin.math.ceil

private const val MAX_HEART_RATE = 190
private val White12 = Color(0x1FFFFFFF)
private val White14 = Color(0x24FFFFFF)

@Composable
fun RideScreen(
    workout: Workout?,
    bike: BikeSource,
    onFinish: (RideSummary) -> Unit,
) {
    val engine = remember { RideEngine(workout, System.currentTimeMillis()) }
    var progress by remember { mutableStateOf(engine.progress()) }
    var paused by remember { mutableStateOf(false) }
    var confirmEnd by remember { mutableStateOf(false) }
    val sample by bike.samples.collectAsState()
    val finish by rememberUpdatedState(onFinish)

    DisposableEffect(engine) {
        bike.target = { workout?.segments?.getOrNull(engine.segmentIndexAt(engine.elapsed)) }
        onDispose { bike.target = { null } }
    }
    LaunchedEffect(engine) {
        while (true) {
            delay(1_000)
            if (paused || confirmEnd) continue
            engine.record(bike.samples.value)
            progress = engine.progress()
            if (engine.isComplete) {
                finish(engine.summary(System.currentTimeMillis()))
                break
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.ride_backdrop),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(listOf(Color(0x8C000000), Color(0x26000000)))),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 64.dp, end = 64.dp, top = 44.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            Header(workout, progress, sample, simulated = bike is SimulatedBike)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(48.dp),
            ) {
                PowerSection(progress, sample, paused, Modifier.weight(1f).fillMaxHeight())
                Column(
                    modifier = Modifier
                        .width(560.dp)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    CadenceTile(progress.segment, sample.rpm, Modifier.weight(1f).fillMaxWidth())
                    HeartTile(sample.heartRate, Modifier.weight(1f).fillMaxWidth())
                    KnobTile(progress.segment, sample.knob, Modifier.weight(1f).fillMaxWidth())
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(172.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                if (workout != null) {
                    TimelineCard(workout, engine, progress, Modifier.weight(1f).fillMaxHeight())
                } else {
                    FreeRideCard(engine, progress, Modifier.weight(1f).fillMaxHeight())
                }
                RoundButton(
                    label = if (paused) "Resume" else "Pause",
                    color = Color(0x993C3C40),
                    edge = true,
                    onClick = { paused = !paused },
                ) {
                    if (paused) Icon(Icons.Play, 44) else Icon(Icons.Pause, 44)
                }
                RoundButton(label = "End", color = Palette.Red, edge = false, onClick = { confirmEnd = true }) {
                    Icon(Icons.Stop, 40, Palette.Ink)
                }
            }
        }

        if (confirmEnd) {
            EndDialog(
                elapsed = progress.elapsed,
                onKeepRiding = { confirmEnd = false },
                onEnd = {
                    confirmEnd = false
                    finish(engine.summary(System.currentTimeMillis()))
                },
            )
        }
    }
}

@Composable
private fun Header(workout: Workout?, progress: RideProgress, sample: BikeSample, simulated: Boolean) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(60.dp),
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .height(56.dp)
                .glass(28, Color(0x8028282C))
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val seg = progress.segment
            T(workout?.name ?: "Free ride", ts(21, 600))
            Dot()
            if (seg == null) {
                T("No plan", ts(21, 600, Palette.Cyan))
            } else {
                val where = if (seg.kind == Kind.Work && progress.workCount > 1) {
                    "${workout?.workLabel ?: seg.label} ${progress.workNumber} of ${progress.workCount}"
                } else {
                    seg.label
                }
                T(where, ts(21, 600, Palette.Cyan))
                Dot()
                T(Format.clock(progress.segmentRemaining), ts(21, 600, tabular = true))
                T("left", ts(21, 400, Palette.Text2))
            }
        }

        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            T(Format.clock(progress.elapsed), ts(60, 700, tracking = -2.2f, lineHeight = 1f, tabular = true))
            val total = progress.total
            if (total != null) {
                T("/ ${Format.clock(total)}", ts(28, 500, Palette.text(0.6f), tabular = true), Modifier.padding(bottom = 4.dp))
            }
        }

        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            val dim = ts(19, 400, Palette.text(0.75f))
            if (simulated) GlassChip("Simulated", height = 40, fontSize = 17, color = Palette.Orange)
            if (sample.heartRate != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Heart, 24, Palette.Red)
                    T("Strap", dim)
                }
            }
            val lean = sample.leanDegrees
            if (lean != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Lean, 24, Palette.text(0.75f))
                    val text = when {
                        lean == 0 -> "Upright"
                        lean < 0 -> "Lean ${-lean}° left"
                        else -> "Lean $lean° right"
                    }
                    T(text, dim)
                }
            }
        }
    }
}

@Composable
private fun Dot() {
    Box(
        Modifier
            .size(4.dp)
            .clip(CircleShape)
            .background(Palette.Text3),
    )
}

private fun effortColor(e: Effort): Color = when (e) {
    Effort.Under -> Palette.Orange
    Effort.InZone -> Palette.Cyan
    Effort.Over -> Palette.Red
}

@Composable
private fun PowerSection(progress: RideProgress, sample: BikeSample, paused: Boolean, modifier: Modifier) {
    val seg = progress.segment
    val target = seg?.targetWatts
    val effort = if (target != null) RideEngine.effort(sample.watts, target) else Effort.InZone
    val color = if (target != null) effortColor(effort) else Palette.Text
    val scale = run {
        val top = maxOf(sample.watts, target?.last ?: 0) + 40
        maxOf(400, (ceil(top / 100.0) * 100).toInt())
    }

    Column(
        modifier = modifier.padding(start = 8.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        T("Watts", ts(26, 600, Palette.text(0.7f)))
        Row(
            modifier = Modifier.padding(top = 8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(40.dp),
        ) {
            T(
                "${sample.watts}",
                ts(390, 800, tracking = -22f, lineHeight = 0.8f, tabular = true),
                Modifier.offset(x = (-14).dp),
                maxLines = 1,
            )
            Column(
                modifier = Modifier.padding(bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (target != null) {
                    T("Target", ts(20, 400, Palette.text(0.7f)))
                    T("${target.first}–${target.last}", ts(52, 700, Palette.Cyan, tracking = -1.6f, lineHeight = 1f, tabular = true))
                } else {
                    T("Ride avg", ts(20, 400, Palette.text(0.7f)))
                    T("${progress.avgWatts}", ts(52, 700, tracking = -1.6f, lineHeight = 1f, tabular = true))
                }
            }
        }

        PowerGauge(sample.watts, target, scale, color, Modifier.padding(top = 52.dp))

        val status = when {
            paused -> "Paused"
            target == null -> "Every pedal stroke counted."
            effort == Effort.Under -> "${target.first - sample.watts} W under. Turn the knob right."
            effort == Effort.Over -> "${sample.watts - target.last} W over. Ease the knob left."
            else -> "In the zone. Hold this."
        }
        T(
            status,
            ts(34, 600, if (paused) Palette.Text2 else color, tracking = -0.6f),
            Modifier.padding(top = 28.dp),
        )
        T(
            "Ride avg ${progress.avgWatts} W · ${progress.kilojoules} kJ · ${Format.miles(progress.miles)} mi",
            ts(22, 400, Palette.text(0.7f), tabular = true),
            Modifier.padding(top = 14.dp),
        )
    }
}

@Composable
private fun PowerGauge(watts: Int, target: IntRange?, scale: Int, color: Color, modifier: Modifier) {
    Column(modifier.width(1040.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(26.dp),
        ) {
            val h = size.height
            val r = CornerRadius(h / 2, h / 2)
            val marker = (watts.toFloat() / scale).coerceIn(0f, 1f) * size.width
            drawRoundRect(White12, cornerRadius = r)
            if (marker > 0f) {
                drawRoundRect(White14, size = Size(marker, h), cornerRadius = r)
            }
            if (target != null) {
                val x0 = target.first.toFloat() / scale * size.width
                val x1 = target.last.toFloat() / scale * size.width
                val edge = 3.dp.toPx()
                drawRect(Color(0x526AD0FF), topLeft = Offset(x0, 0f), size = Size(x1 - x0, h))
                drawRect(Palette.Cyan, topLeft = Offset(x0, 0f), size = Size(edge, h))
                drawRect(Palette.Cyan, topLeft = Offset(x1 - edge, 0f), size = Size(edge, h))
            }
            val knob = 26.dp.toPx()
            drawCircle(Palette.Text, radius = knob, center = Offset(marker, h / 2))
            drawCircle(color, radius = knob - 5.dp.toPx(), center = Offset(marker, h / 2))
        }
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(22.dp),
        ) {
            val style = ts(16, 400, Palette.text(0.55f), tabular = true)
            (0..4).forEach { k ->
                val label = if (k == 4) "$scale W" else "${scale * k / 4}"
                val w = 80.dp
                val x = when (k) {
                    0 -> 0.dp
                    4 -> maxWidth - w
                    else -> maxWidth * (k / 4f) - w / 2
                }
                T(
                    label,
                    style,
                    Modifier
                        .offset(x = x)
                        .width(w),
                    align = when (k) {
                        0 -> androidx.compose.ui.text.style.TextAlign.Start
                        4 -> androidx.compose.ui.text.style.TextAlign.End
                        else -> androidx.compose.ui.text.style.TextAlign.Center
                    },
                )
            }
        }
    }
}

@Composable
private fun Tile(modifier: Modifier, vertical: Int = 26, content: @Composable () -> Unit) {
    Box(
        modifier
            .glass(36)
            .padding(horizontal = 32.dp, vertical = vertical.dp),
    ) { content() }
}

@Composable
private fun BigValue(value: String, unit: String) {
    Row(Modifier.width(210.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        T(value, ts(116, 800, tracking = -6f, lineHeight = 0.9f, tabular = true), maxLines = 1)
        T(unit, ts(22, 400, Palette.text(0.7f)), Modifier.padding(bottom = 8.dp))
    }
}

@Composable
private fun CadenceTile(seg: Segment?, rpm: Int, modifier: Modifier) {
    Tile(modifier) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            T("Cadence", ts(20, 600, Palette.text(0.7f)))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                BigValue("$rpm", "rpm")
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val inRange = seg?.rpmInRange(rpm) ?: true
                    val color = if (inRange) Palette.Cyan else Palette.Orange
                    Canvas(
                        Modifier
                            .fillMaxWidth()
                            .height(12.dp),
                    ) {
                        val h = size.height
                        fun x(v: Int) = ((v - 40) / 80f).coerceIn(0f, 1f) * size.width
                        drawRoundRect(White12, cornerRadius = CornerRadius(h / 2, h / 2))
                        val lo = seg?.rpmLow
                        val hi = seg?.rpmHigh ?: 120
                        if (lo != null) {
                            drawRoundRect(
                                Color(0x736AD0FF),
                                topLeft = Offset(x(lo), 0f),
                                size = Size(x(hi) - x(lo), h),
                                cornerRadius = CornerRadius(h / 2, h / 2),
                            )
                        }
                        val c = Offset(x(rpm), h / 2)
                        drawCircle(Palette.Text, radius = 15.dp.toPx(), center = c)
                        drawCircle(color, radius = 11.dp.toPx(), center = c)
                    }
                    val text = when {
                        seg == null || seg.rpmLow == null -> "Any pace"
                        inRange -> "In range ${seg.rpmLabel}"
                        rpm < seg.rpmLow -> "Pick it up · ${seg.rpmLabel}"
                        else -> "Ease off · ${seg.rpmLabel}"
                    }
                    T(text, ts(18, 600, color))
                }
            }
        }
    }
}

@Composable
private fun HeartTile(heartRate: Int?, modifier: Modifier) {
    Tile(modifier) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                T("Heart rate", ts(20, 600, Palette.text(0.7f)), Modifier.weight(1f))
                Icon(Icons.Heart, 24, if (heartRate != null) Palette.Red else Palette.Text3)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                BigValue(heartRate?.toString() ?: "–", "bpm")
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val zone = if (heartRate == null) 0 else zoneFor(heartRate)
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        (1..5).forEach { z ->
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (z == zone) Palette.Red else White14),
                            )
                        }
                    }
                    T(
                        if (heartRate == null) "No strap connected" else "Zone $zone · ${zoneNames[zone - 1]}",
                        ts(18, 600, if (heartRate == null) Palette.Text2 else Palette.Text),
                    )
                }
            }
        }
    }
}

private val zoneNames = listOf("Recovery", "Endurance", "Tempo", "Threshold", "Max")

private fun zoneFor(hr: Int): Int {
    val pct = hr.toFloat() / MAX_HEART_RATE
    return when {
        pct < 0.6f -> 1
        pct < 0.7f -> 2
        pct < 0.8f -> 3
        pct < 0.9f -> 4
        else -> 5
    }
}

private fun knobDeg(v: Int): Float = 135f + (v.coerceIn(1, 100) - 1) / 99f * 270f

@Composable
private fun KnobTile(seg: Segment?, knob: Int, modifier: Modifier) {
    val cue = if (seg != null) RideEngine.cue(knob, seg.knob) else null
    val color = when (cue?.first) {
        Cue.Right -> Palette.Orange
        Cue.Left -> Palette.Red
        Cue.Hold -> Palette.Cyan
        null -> Palette.Text
    }
    Tile(modifier, vertical = 22) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            Box(Modifier.size(156.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val u = size.width / 200f
                    val r = 84 * u
                    val topLeft = Offset(center.x - r, center.y - r)
                    val arc = Size(r * 2, r * 2)
                    val stroke = Stroke(22 * u, cap = StrokeCap.Round)
                    drawArc(White14, 135f, 270f, false, topLeft, arc, style = stroke)
                    drawArc(color, 135f, knobDeg(knob) - 135f, false, topLeft, arc, style = stroke)
                    if (seg != null) {
                        val a = Math.toRadians(knobDeg(seg.knob).toDouble())
                        val cos = kotlin.math.cos(a).toFloat()
                        val sin = kotlin.math.sin(a).toFloat()
                        drawLine(
                            Palette.Text,
                            Offset(center.x + 68 * u * cos, center.y + 68 * u * sin),
                            Offset(center.x + 100 * u * cos, center.y + 100 * u * sin),
                            strokeWidth = 6 * u,
                            cap = StrokeCap.Round,
                        )
                    }
                }
                T("$knob", ts(58, 800, tracking = -2f, lineHeight = 1f, tabular = true))
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                T(if (seg != null) "Knob · target ${seg.knob}" else "Knob", ts(20, 600, Palette.text(0.7f)))
                if (cue != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        val (icon, label, value) = when (cue.first) {
                            Cue.Right -> Triple(Icons.TurnRight, "Turn right", "+${cue.second}")
                            Cue.Left -> Triple(Icons.TurnLeft, "Turn left", "−${cue.second}")
                            Cue.Hold -> Triple(Icons.Check, "Hold", "${seg!!.knob}")
                        }
                        Icon(icon, 30, color)
                        T(label, ts(24, 600, color))
                        T(value, ts(56, 800, color, tracking = -2f, lineHeight = 1f, tabular = true))
                    }
                } else {
                    T("Set it where it feels good.", ts(22, 500, Palette.Text2))
                }
            }
        }
    }
}

@Composable
private fun TimelineCard(workout: Workout, engine: RideEngine, progress: RideProgress, modifier: Modifier) {
    Column(
        modifier = modifier
            .glass(36)
            .padding(start = 32.dp, end = 32.dp, top = 24.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            val next = progress.next
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Bottom) {
                if (next != null) {
                    T("Next", ts(21, 600, Palette.text(0.6f)))
                    T("${next.label} · ${Format.clock(next.seconds)}", ts(21, 600))
                    T("knob", ts(21, 400, Palette.text(0.7f)))
                    T("${next.knob}", ts(21, 700, Palette.Cyan, tabular = true))
                    T("· ${next.rpmLabel} rpm", ts(21, 400, Palette.text(0.7f)))
                } else {
                    T("Last one", ts(21, 600, Palette.text(0.6f)))
                    T("Finish strong", ts(21, 600))
                }
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                T(Format.clock(workout.totalSeconds - progress.elapsed), ts(26, 700, tracking = -0.5f, tabular = true))
                T("to go", ts(19, 400, Palette.text(0.7f)), Modifier.padding(bottom = 2.dp))
            }
        }
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            val current = progress.segmentIndex
            ProfileBars(
                segments = workout.segments,
                height = 70,
                gap = 5,
                radius = 7,
                barHeight = { 14f + it.effort * 56f },
                color = { i, _ ->
                    when {
                        i == current -> Palette.Cyan
                        engine.segmentEnd(i) <= progress.elapsed -> Color(0x666AD0FF)
                        else -> White14
                    }
                },
                modifier = Modifier.align(Alignment.BottomStart),
            )
            val pct = (progress.elapsed.toFloat() / workout.totalSeconds).coerceIn(0f, 1f)
            val x = maxWidth * pct
            Box(
                Modifier
                    .offset(x = x - 2.dp, y = 4.dp)
                    .width(4.dp)
                    .fillMaxHeight()
                    .padding(bottom = 0.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Palette.Text),
            )
            Box(
                Modifier
                    .offset(x = x - 8.dp, y = (-4).dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Palette.Text),
            )
        }
    }
}

@Composable
private fun FreeRideCard(engine: RideEngine, progress: RideProgress, modifier: Modifier) {
    Column(
        modifier = modifier
            .glass(36)
            .padding(start = 32.dp, end = 32.dp, top = 24.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            T("Power", ts(21, 600, Palette.text(0.6f)), Modifier.weight(1f))
            T("${progress.kilojoules} kJ", ts(26, 700, tracking = -0.5f, tabular = true))
        }
        val trace = engine.seconds.map { it.watts }
        Canvas(
            Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            if (trace.size < 2) return@Canvas
            val window = 1200 // last 20 minutes
            val shown = if (trace.size > window) trace.subList(trace.size - window, trace.size) else trace
            val max = (shown.maxOrNull() ?: 1).coerceAtLeast(100) * 1.1f
            val path = Path()
            shown.forEachIndexed { i, v ->
                val x = size.width * i / (shown.size - 1).coerceAtLeast(1)
                val y = size.height - v / max * size.height
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, Palette.Orange, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

@Composable
private fun RoundButton(
    label: String,
    color: Color,
    edge: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
    ) {
        Box(
            modifier = Modifier
                .size(128.dp)
                .pressable(onClick = onClick)
                .glass(64, color, edge),
            contentAlignment = Alignment.Center,
        ) { icon() }
        T(label, ts(19, 600, Palette.text(0.8f)))
    }
}

@Composable
private fun EndDialog(elapsed: Int, onKeepRiding: () -> Unit, onEnd: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onKeepRiding),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(720.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .card(Palette.Surface1, 40)
                .padding(48.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            T("End this ride?", ts(52, 700, tracking = -1.6f, lineHeight = 1f))
            T(
                if (elapsed < 60) "Rides under a minute aren't saved." else "You've ridden ${Format.clock(elapsed)}. It will be saved on the bike.",
                ts(24, 400, Palette.Text2, lineHeight = 1.4f),
            )
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SecondaryPill("Keep riding", Modifier.weight(1f), height = 88, fontSize = 24, onClick = onKeepRiding)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(88.dp)
                        .pressable(onClick = onEnd)
                        .clip(RoundedCornerShape(44.dp))
                        .background(Palette.Red),
                    contentAlignment = Alignment.Center,
                ) { T("End ride", ts(24, 600, Palette.Ink)) }
            }
        }
    }
}
