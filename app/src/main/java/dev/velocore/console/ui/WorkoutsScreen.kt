package dev.velocore.console.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import dev.velocore.console.data.Category
import dev.velocore.console.data.Segment
import dev.velocore.console.data.Workout
import dev.velocore.console.data.Workouts

@Composable
fun WorkoutsScreen(
    initialSelection: String,
    onBack: () -> Unit,
    onStart: (Workout) -> Unit,
    onNotYet: (String) -> Unit,
) {
    var selectedId by remember { mutableStateOf(initialSelection) }
    var category by remember { mutableStateOf<Category?>(null) }
    val shown = Workouts.all.filter { category == null || it.category == category }
    val selected = Workouts.byId(selectedId) ?: Workouts.hill

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 64.dp, end = 64.dp, top = 44.dp, bottom = 52.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .pressable(onClick = onBack)
                    .clip(CircleShape)
                    .background(Palette.Surface2),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Back, 28) }
            T("Workouts", ts(60, 700, tracking = -2f, lineHeight = 1f))
            Spacer(Modifier.weight(1f))
            Segmented(category) { category = it }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            WorkoutGrid(shown, selected.id, Modifier.weight(1f).fillMaxHeight()) { selectedId = it.id }
            Detail(
                workout = selected,
                modifier = Modifier
                    .width(640.dp)
                    .fillMaxHeight(),
                onStart = { onStart(selected) },
                onWithShow = { onNotYet("Riding with a show is coming in a later build.") },
            )
        }
    }
}

@Composable
private fun Segmented(selected: Category?, onPick: (Category?) -> Unit) {
    Row(
        modifier = Modifier
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(Palette.Surface2)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        val options: List<Category?> = listOf(null) + Category.entries
        options.forEach { c ->
            val on = c == selected
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .pressable { onPick(c) }
                    .clip(RoundedCornerShape(28.dp))
                    .background(if (on) Palette.Surface4 else Color.Transparent)
                    .padding(horizontal = 28.dp),
                contentAlignment = Alignment.Center,
            ) {
                T(c?.label ?: "All", ts(20, 600, if (on) Palette.Text else Palette.Text2))
            }
        }
    }
}

/** Three columns of 278 px cards, like the canvas. */
@Composable
private fun WorkoutGrid(items: List<Workout>, selectedId: String, modifier: Modifier, onPick: (Workout) -> Unit) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        items.chunked(3).forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(278.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                row.forEach { w ->
                    WorkoutCard(w, w.id == selectedId, Modifier.weight(1f).fillMaxHeight()) { onPick(w) }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun WorkoutCard(w: Workout, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .pressable(onClick = onClick)
            .drawBehind {
                if (selected) {
                    val out = 7.dp.toPx()
                    drawRoundRect(
                        color = Palette.Text,
                        topLeft = Offset(-out, -out),
                        size = Size(size.width + out * 2, size.height + out * 2),
                        cornerRadius = CornerRadius(30.dp.toPx() + out),
                        style = Stroke(4.dp.toPx()),
                    )
                }
            }
            .card(Palette.Surface1, 30),
    ) {
        Cover(w.cover)
        BottomScrim(0.78f, 0.88f)
        GlassChip(
            text = w.level,
            height = 36,
            fontSize = 16,
            color = if (w.level == "Max") Palette.Orange else Palette.Text,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(18.dp),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 26.dp, end = 26.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            T(w.name, ts(32, 700, tracking = -1f, lineHeight = 1.1f), maxLines = 1)
            T(w.meta, ts(18, 400, Palette.text(0.78f)))
            ProfileBars(
                segments = w.segments,
                height = 30,
                gap = 3,
                radius = 3,
                barHeight = { 5f + it.effort * 25f },
                color = { _, s -> glassFor(s.effort) },
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

@Composable
private fun Detail(workout: Workout, modifier: Modifier, onStart: () -> Unit, onWithShow: () -> Unit) {
    Column(modifier.card(Palette.Surface1, 36)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(250.dp),
        ) {
            Cover(workout.cover)
            BottomScrim(0.85f, 1f, Palette.Surface1)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 36.dp, end = 36.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                T(workout.category.label, ts(19, 600, Palette.text(0.8f)))
                T(workout.name, ts(60, 700, tracking = -2f, lineHeight = 1f), maxLines = 1)
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(start = 36.dp, end = 36.dp, top = 14.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                T("${workout.minutes} min · ${workout.level} · ${workout.extra}", ts(22, 600))
                T(workout.description, ts(20, 400, Palette.Text2, lineHeight = 1.45f))
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileBars(
                    segments = workout.segments,
                    height = 100,
                    gap = 3,
                    radius = 5,
                    barHeight = { 10f + it.effort * 90f },
                    color = { _, s -> fillFor(s.effort) },
                )
                Ticks(workout.minutes)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                PlanRowLayout(
                    listOf("Segment", "Time", "Knob", "Cadence"),
                    Modifier.padding(start = 2.dp, end = 2.dp, bottom = 8.dp),
                    header = true,
                )
                workout.plan.forEach { r ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Palette.Hairline),
                    )
                    PlanRowLayout(
                        listOf(r.name, r.time, r.knob, r.rpm),
                        Modifier
                            .height(46.dp)
                            .padding(horizontal = 2.dp),
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                PrimaryPill("Start", Modifier.weight(1.2f), onClick = onStart)
                SecondaryPill("With a show", Modifier.weight(1f), height = 88, icon = Icons.Tv, onClick = onWithShow)
            }
        }
    }
}

@Composable
private fun PlanRowLayout(cells: List<String>, modifier: Modifier, header: Boolean = false) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (header) {
            val h = ts(15, 600, Palette.Text3)
            T(cells[0], h, Modifier.weight(1f))
            T(cells[1], h, Modifier.width(128.dp))
            T(cells[2], h, Modifier.width(100.dp))
            T(cells[3], h, Modifier.width(110.dp))
        } else {
            T(cells[0], ts(19, 600), Modifier.weight(1f), maxLines = 1)
            T(cells[1], ts(19, 400, Palette.Text2, tabular = true), Modifier.width(128.dp), maxLines = 1)
            T(cells[2], ts(19, 700, Palette.Cyan, tabular = true), Modifier.width(100.dp), maxLines = 1)
            T(cells[3], ts(19, 400, Palette.Text2, tabular = true), Modifier.width(110.dp), maxLines = 1)
        }
    }
}

@Composable
private fun Ticks(minutes: Int) {
    val every = when {
        minutes <= 20 -> 5
        minutes <= 45 -> 10
        else -> 15
    }
    val marks = (0..minutes step every).toMutableList()
    if (minutes % every != 0) marks += minutes
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(20.dp),
    ) {
        val full = maxWidth
        marks.forEach { m ->
            val frac = m.toFloat() / minutes
            val label = "$m min"
            // Approximate label width so the first mark sits left, the last right, the rest centered.
            val labelWidth = (label.length * 8.5f).dp
            val x = when (m) {
                0 -> 0.dp
                minutes -> full - labelWidth
                else -> full * frac - labelWidth / 2
            }
            T(
                label,
                ts(15, 400, Palette.Text3, tabular = true),
                Modifier
                    .offset(x = x)
                    .width(labelWidth),
                maxLines = 1,
                align = when (m) {
                    0 -> androidx.compose.ui.text.style.TextAlign.Start
                    minutes -> androidx.compose.ui.text.style.TextAlign.End
                    else -> androidx.compose.ui.text.style.TextAlign.Center
                },
            )
        }
    }
}

/** Bars whose widths follow each segment's length, like the canvas's flex rows. */
@Composable
fun ProfileBars(
    segments: List<Segment>,
    height: Int,
    gap: Int,
    radius: Int,
    barHeight: (Segment) -> Float,
    color: (index: Int, segment: Segment) -> Color,
    modifier: Modifier = Modifier,
) {
    Layout(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp),
        content = {
            segments.forEachIndexed { i, s ->
                Box(
                    Modifier
                        .clip(RoundedCornerShape(radius.dp))
                        .background(color(i, s)),
                )
            }
        },
    ) { measurables, constraints ->
        val gapPx = gap.dp.roundToPx()
        val total = segments.sumOf { it.seconds }.toFloat()
        val usable = (constraints.maxWidth - gapPx * (segments.size - 1)).coerceAtLeast(0)
        var x = 0f
        val placed = measurables.mapIndexed { i, m ->
            val w = usable * segments[i].seconds / total
            val h = barHeight(segments[i]).dp.roundToPx().coerceAtMost(constraints.maxHeight)
            val left = x.toInt()
            x += w + gapPx
            val right = (x - gapPx).toInt().coerceAtLeast(left + 1)
            Triple(m.measure(Constraints.fixed(right - left, h)), left, h)
        }
        layout(constraints.maxWidth, constraints.maxHeight) {
            placed.forEach { (p, left, h) -> p.place(left, constraints.maxHeight - h) }
        }
    }
}

fun fillFor(e: Float): Color = when {
    e >= 0.75f -> Palette.Cyan
    e >= 0.5f -> Palette.CyanMid
    else -> Palette.CyanLow
}

fun glassFor(e: Float): Color = when {
    e >= 0.75f -> Palette.text(0.95f)
    e >= 0.5f -> Palette.text(0.6f)
    else -> Palette.text(0.35f)
}
