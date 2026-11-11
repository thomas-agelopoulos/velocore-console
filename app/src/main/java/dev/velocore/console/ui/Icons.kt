package dev.velocore.console.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp

/** The line icons from the design, drawn on a 24 x 24 grid. */
object Icons {
    val Play = fill("play", "M8 5.5v13a1 1 0 0 0 1.5.9l10.2-6.5a1 1 0 0 0 0-1.7L9.5 4.6A1 1 0 0 0 8 5.5Z")
    val Heart = fill("heart", "M19.5 12.6 12 20l-7.5-7.4A4.8 4.8 0 0 1 12 6.5a4.8 4.8 0 0 1 7.5 6.1Z")
    val Stop = fill("stop", "M8 5h8a3 3 0 0 1 3 3v8a3 3 0 0 1-3 3H8a3 3 0 0 1-3-3V8a3 3 0 0 1 3-3Z")
    val Pause = fill(
        "pause",
        "M7.5 5h1a1.5 1.5 0 0 1 1.5 1.5v11a1.5 1.5 0 0 1-1.5 1.5h-1A1.5 1.5 0 0 1 6 17.5v-11A1.5 1.5 0 0 1 7.5 5Z",
        "M15.5 5h1a1.5 1.5 0 0 1 1.5 1.5v11a1.5 1.5 0 0 1-1.5 1.5h-1A1.5 1.5 0 0 1 14 17.5v-11A1.5 1.5 0 0 1 15.5 5Z",
    )
    val Wifi = stroke(
        "wifi", 2f,
        "M5 12.5a10 10 0 0 1 14 0", "M8.5 16a5 5 0 0 1 7 0", "M2 9a14 14 0 0 1 20 0", circle(12f, 19.5f, 1f),
    )
    val Tv = stroke(
        "tv", 2f,
        "M5 5h14a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2Z", "M8 21h8", "M12 17v4",
    )
    val History = stroke("history", 2f, "M3 12a9 9 0 1 0 2.6-6.4L3 8", "M3 3v5h5", "M12 7v5l3 2")
    val Replay = stroke("replay", 2f, "M3 12a9 9 0 1 0 2.6-6.4L3 8", "M3 3v5h5")
    val Gear = stroke(
        "gear", 2f,
        circle(12f, 12f, 3f),
        "M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1Z",
    )
    val Back = stroke("back", 2.4f, "M15 5l-7 7 7 7")
    val Check = stroke("check", 2.6f, "M5 12.5l4.5 4.5L19 7.5")
    val CheckCircle = stroke("checkCircle", 2.2f, circle(12f, 12f, 9.5f), "M7.5 12.3l3 3 6-6.3")
    val Download = stroke("download", 2f, "M12 3v12", "M7 10l5 5 5-5", "M4 21h16")
    val Lean = stroke("lean", 2f, "M4 18a8 8 0 0 1 16 0", "M12 18l-3-6")
    val TurnRight = stroke("turnRight", 2.4f, "M20 12a8 8 0 1 1-2.3-5.7", "M20 4v4h-4")
    val TurnLeft = stroke("turnLeft", 2.4f, "M4 12a8 8 0 1 0 2.3-5.7", "M4 4v4h4")

    private fun circle(cx: Float, cy: Float, r: Float): String =
        "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0Z"

    private fun fill(name: String, vararg paths: String): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            paths.forEach { addPath(pathData = addPathNodes(it), fill = SolidColor(Color.White)) }
        }.build()

    private fun stroke(name: String, width: Float, vararg paths: String): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            paths.forEach {
                addPath(
                    pathData = addPathNodes(it),
                    stroke = SolidColor(Color.White),
                    strokeLineWidth = width,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()
}

@Composable
fun Icon(vector: ImageVector, size: Int, color: Color = Palette.Text, modifier: Modifier = Modifier) {
    Image(
        painter = rememberVectorPainter(vector),
        contentDescription = null,
        modifier = modifier.size(size.dp),
        colorFilter = ColorFilter.tint(color),
    )
}
