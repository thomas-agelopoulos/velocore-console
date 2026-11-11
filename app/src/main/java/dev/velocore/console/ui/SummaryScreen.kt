package dev.velocore.console.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import dev.velocore.console.R
import dev.velocore.console.data.RideRecord
import dev.velocore.console.data.Workout
import dev.velocore.console.ride.RideSummary
import dev.velocore.console.ride.SegmentResult
import kotlin.math.ceil
import kotlin.math.roundToInt

@Composable
fun SummaryScreen(
    summary: RideSummary,
    workout: Workout?,
    previous: RideRecord?,
    saved: Boolean,
    onDone: () -> Unit,
    onRideAgain: () -> Unit,
    onNotYet: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 56.dp, vertical = 44.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Banner(summary, workout, saved)
        Totals(summary, previous)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            PowerChart(summary, workout, Modifier.weight(1f).fillMaxHeight())
            ResultsTable(summary, workout, Modifier.width(620.dp).fillMaxHeight())
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SecondaryPill("Export file", height = 88, icon = Icons.Download) {
                onNotYet("Exporting ride files is coming in a later build.")
            }
            Spacer(Modifier.weight(1f))
            SecondaryPill("Ride again", height = 88, icon = Icons.Replay, onClick = onRideAgain)
            PrimaryPill("Done", Modifier.width(320.dp), icon = null, onClick = onDone)
        }
    }
}

private fun plural(label: String) = label.lowercase() + "s"

@Composable
private fun Banner(summary: RideSummary, workout: Workout?, saved: Boolean) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(280.dp)
            .card(Palette.Surface1, 40),
    ) {
        Cover(workout?.cover ?: R.drawable.cover_road, verticalBias = -0.3f)
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.7f)
                .background(Brush.horizontalGradient(listOf(Color(0xC7000000), Color(0x00000000)))),
        )
        BottomScrim(0.6f, 0.6f)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 52.dp, bottom = 42.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.CheckCircle, 26, Palette.Cyan)
                T("Ride complete", ts(21, 600, Palette.Cyan))
            }
            T(summary.name, ts(92, 700, tracking = -3.4f, lineHeight = 0.95f), maxLines = 1)
            val parts = mutableListOf(Format.span(summary.startedAt, summary.endedAt), Format.clock(summary.seconds))
            if (summary.workTotal > 1) {
                parts += if (summary.workDone >= summary.workTotal) {
                    "all ${summary.workTotal} ${plural(summary.workLabel)}"
                } else {
                    "${summary.workDone} of ${summary.workTotal} ${plural(summary.workLabel)}"
                }
            }
            T(parts.joinToString(" · "), ts(23, 400, Palette.text(0.82f)))
        }
        if (saved) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(32.dp)
                    .height(48.dp)
                    .glass(24, Palette.Glass, edge = false)
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Check, 20, Palette.Cyan)
                T("Saved on the bike", ts(18, 600))
            }
        }
    }
}

@Composable
private fun Totals(summary: RideSummary, previous: RideRecord?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        val tile = Modifier
            .weight(1f)
            .fillMaxHeight()
        val delta = previous?.let { summary.avgWatts - it.avgWatts }
        TotalTile("Output", "${summary.kilojoules}", "kJ", tile, valueColor = Palette.Orange)
        TotalTile(
            "Avg power", "${summary.avgWatts}", "W", tile,
            note = delta?.let { if (it >= 0) "+$it vs last" else "−${-it} vs last" },
            noteColor = if ((delta ?: 0) >= 0) Palette.Cyan else Palette.Text3,
        )
        TotalTile("Avg cadence", "${summary.avgRpm}", "rpm", tile)
        TotalTile(
            "Avg heart rate", summary.avgHeartRate?.toString() ?: "–", "bpm", tile,
            note = summary.maxHeartRate?.let { "max $it" }, noteColor = Palette.Text3,
        )
        TotalTile("Calories", "${summary.calories}", "kcal", tile)
        TotalTile("Distance", Format.miles(summary.miles), "mi", tile)
    }
}

@Composable
private fun TotalTile(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier,
    valueColor: Color = Palette.Text,
    note: String? = null,
    noteColor: Color = Palette.Text3,
) {
    Column(
        modifier = modifier
            .card(Palette.Surface1, 28)
            .padding(horizontal = 28.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            T(label, ts(18, 600, Palette.Text2), Modifier.weight(1f), maxLines = 1)
            if (note != null) T(note, ts(16, 600, noteColor), maxLines = 1)
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            T(value, ts(62, 700, valueColor, tracking = -2.4f, lineHeight = 0.9f, tabular = true), maxLines = 1)
            T(unit, ts(20, 400, Palette.Text2), Modifier.padding(bottom = 4.dp))
        }
    }
}

@Composable
private fun PowerChart(summary: RideSummary, workout: Workout?, modifier: Modifier) {
    val measurer = rememberTextMeasurer()
    val labelStyle = ts(16, 400, Palette.Text3, tabular = true)
    val smooth = remember(summary) { smoothTrace(summary.trace, 5, 360) }
    Column(
        modifier = modifier
            .card(Palette.Surface1, 36)
            .padding(start = 32.dp, end = 32.dp, top = 26.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            T("Power", ts(24, 600), Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                if (workout != null) {
                    Legend(Modifier.size(24.dp, 14.dp).clip(RoundedCornerShape(4.dp)).background(Palette.CyanBand), "Target")
                }
                Legend(Modifier.size(24.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(Palette.Orange), "You")
            }
        }
        Canvas(
            Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            val left = 52.dp.toPx()
            val right = size.width - 8.dp.toPx()
            val top = 12.dp.toPx()
            val bottom = size.height - 36.dp.toPx()
            val peak = maxOf(smooth.maxOrNull() ?: 0, summary.results.maxOfOrNull { it.target?.last ?: 0 } ?: 0)
            val maxW = maxOf(350, (ceil((peak + 20) / 50.0) * 50).toInt())
            val seconds = summary.seconds.coerceAtLeast(1)
            fun y(w: Int) = bottom - w * (bottom - top) / maxW
            fun x(sec: Int) = left + sec.toFloat() / seconds * (right - left)

            var g = 100
            while (g < maxW) {
                drawLine(Color(0xFF1F1F22), Offset(left, y(g)), Offset(right, y(g)), strokeWidth = 1.5.dp.toPx())
                val t = measurer.measure("$g", labelStyle)
                drawText(t, topLeft = Offset(left - 12.dp.toPx() - t.size.width, y(g) - t.size.height / 2f))
                g += 100
            }
            drawLine(Palette.Surface3, Offset(left, bottom), Offset(right, bottom), strokeWidth = 2.dp.toPx())

            if (workout != null) {
                var start = 0
                workout.segments.forEach { s ->
                    val end = start + s.seconds
                    if (start < summary.seconds) {
                        val band = s.targetWatts
                        val x0 = x(start) + 2.dp.toPx()
                        val x1 = x(minOf(end, summary.seconds)) - 2.dp.toPx()
                        if (x1 > x0) {
                            drawRect(
                                Palette.CyanBand,
                                topLeft = Offset(x0, y(band.last)),
                                size = Size(x1 - x0, y(band.first) - y(band.last)),
                            )
                        }
                    }
                    start = end
                }
            }

            if (smooth.size >= 2) {
                val path = Path()
                smooth.forEachIndexed { i, w ->
                    val px = left + i.toFloat() / (smooth.size - 1) * (right - left)
                    val py = y(w)
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                drawPath(path, Palette.Orange, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }

            val minutes = seconds / 60f
            val every = when {
                minutes <= 12 -> 2
                minutes <= 35 -> 5
                minutes <= 70 -> 10
                else -> 15
            }
            var m = 0
            while (m * 60 <= seconds) {
                val last = (m + every) * 60 > seconds
                val text = if (last && m > 0) "$m min" else "$m"
                val t = measurer.measure(text, labelStyle)
                val cx = x(m * 60)
                val tx = when {
                    m == 0 -> cx
                    cx + t.size.width / 2f > right -> right - t.size.width
                    else -> cx - t.size.width / 2f
                }
                drawText(t, topLeft = Offset(tx, bottom + 14.dp.toPx()))
                m += every
            }
        }
    }
}

@Composable
private fun Legend(swatch: Modifier, label: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(swatch)
        T(label, ts(17, 400, Palette.Text2))
    }
}

/** Moving average, then thinned to at most [points] values, so the line reads as a ride, not noise. */
private fun smoothTrace(trace: List<Int>, window: Int, points: Int): List<Int> {
    if (trace.isEmpty()) return emptyList()
    val avg = trace.indices.map { i ->
        val from = (i - window / 2).coerceAtLeast(0)
        val to = (i + window / 2 + 1).coerceAtMost(trace.size)
        trace.subList(from, to).average().roundToInt()
    }
    if (avg.size <= points) return avg
    return (0 until points).map { k -> avg[(k.toLong() * (avg.size - 1) / (points - 1)).toInt()] }
}

@Composable
private fun ResultsTable(summary: RideSummary, workout: Workout?, modifier: Modifier) {
    val isPlan = workout != null
    Column(
        modifier = modifier
            .card(Palette.Surface1, 36)
            .padding(horizontal = 32.dp, vertical = 26.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            val title = if (isPlan) plural(summary.workLabel).replaceFirstChar { it.uppercase() } else "Splits"
            T(title, ts(24, 600), Modifier.weight(1f))
            val on = summary.onTargetPct
            if (isPlan && on != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
                    T("$on%", ts(18, 700, Palette.Cyan))
                    T("of ${summary.workLabel.lowercase()} time on target", ts(18, 400, Palette.Text2))
                }
            } else if (!isPlan) {
                T("every 5 minutes", ts(18, 400, Palette.Text2))
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 2.dp, end = 2.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val h = ts(15, 600, Palette.Text3)
            T("Segment", h, Modifier.weight(1f))
            T(if (isPlan) "Target W" else "Avg rpm", h, Modifier.width(130.dp))
            T("Avg W", h, Modifier.width(90.dp))
            T(if (isPlan) "On target" else "Effort", h, Modifier.width(150.dp))
        }
        val best = summary.results.maxOfOrNull { it.avgWatts }?.coerceAtLeast(1) ?: 1
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            if (summary.results.isEmpty()) {
                T("Nothing to show yet.", ts(20, 400, Palette.Text2), Modifier.padding(top = 12.dp))
            }
            summary.results.forEach { r -> ResultRow(r, isPlan, best) }
        }
    }
}

@Composable
private fun ResultRow(r: SegmentResult, isPlan: Boolean, best: Int) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Palette.Hairline),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        T(r.name, ts(20, 600), Modifier.weight(1f), maxLines = 1)
        val second = if (isPlan) r.target?.let { "${it.first}–${it.last}" } ?: "–" else "${r.avgRpm}"
        T(second, ts(20, 400, Palette.Text2, tabular = true), Modifier.width(130.dp), maxLines = 1)
        T("${r.avgWatts}", ts(20, 700, Palette.Orange, tabular = true), Modifier.width(90.dp), maxLines = 1)
        val pct = r.onTargetPct ?: (r.avgWatts * 100 / best)
        Row(
            modifier = Modifier.width(150.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Palette.Surface2),
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth((pct / 100f).coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(4.dp))
                        .background(Palette.Cyan),
                )
            }
            T(
                if (isPlan) "$pct%" else "",
                ts(17, 400, Palette.Text2, tabular = true),
                Modifier.width(44.dp),
                align = androidx.compose.ui.text.style.TextAlign.End,
            )
        }
    }
}
