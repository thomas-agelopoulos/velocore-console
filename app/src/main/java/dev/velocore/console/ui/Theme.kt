package dev.velocore.console.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.velocore.console.R
import kotlin.math.min

/** Colors from the design canvas. */
object Palette {
    val Ink = Color(0xFF000000)
    val Surface1 = Color(0xFF111113)
    val Surface2 = Color(0xFF1C1C1F)
    val Surface3 = Color(0xFF2A2A2E)
    val Surface4 = Color(0xFF3A3A3E)
    val Hairline = Color(0xFF222225)
    val Text = Color(0xFFF5F5F7)
    val Text2 = Color(0xFFA1A1A6)
    val Text3 = Color(0xFF8E8E93)
    val Cyan = Color(0xFF6AD0FF)
    val CyanTrack = Color(0xFF13303D)
    val CyanBand = Color(0xFF154456)
    val CyanMid = Color(0xFF3C7C9C)
    val CyanLow = Color(0xFF24485A)
    val Orange = Color(0xFFFFA43A)
    val OrangeArea = Color(0xFF2B1D0C)
    val Red = Color(0xFFFF6B63)

    // Android 9 can't blur what's behind a view, so glass is a touch more opaque than on the canvas.
    val Glass = Color(0xB828282C)
    val GlassDark = Color(0x9E1C1C1E)
    val GlassEdge = Color(0x14FFFFFF)

    fun text(alpha: Float) = Text.copy(alpha = alpha)
}

val Inter = FontFamily(
    Font(R.font.inter_400, FontWeight.Normal),
    Font(R.font.inter_500, FontWeight.Medium),
    Font(R.font.inter_600, FontWeight.SemiBold),
    Font(R.font.inter_700, FontWeight.Bold),
    Font(R.font.inter_800, FontWeight.ExtraBold),
)

/**
 * Text style in canvas units. Sizes and letter spacing are the CSS pixel values from the design;
 * [DesignCanvas] makes 1 sp equal one canvas pixel. [lineHeight] is the CSS line-height multiplier.
 */
fun ts(
    size: Int,
    weight: Int = 400,
    color: Color = Palette.Text,
    tracking: Float = 0f,
    lineHeight: Float? = null,
    tabular: Boolean = false,
): TextStyle = TextStyle(
    fontFamily = Inter,
    fontSize = size.sp,
    fontWeight = FontWeight(weight),
    color = color,
    letterSpacing = tracking.sp,
    lineHeight = if (lineHeight != null) (size * lineHeight).sp else TextStyle.Default.lineHeight,
    lineHeightStyle = if (lineHeight != null) {
        // Trim.None so a line height smaller than the font shrinks the box, like CSS does.
        LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)
    } else {
        null
    },
    fontFeatureSettings = if (tabular) "tnum" else null,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

/**
 * Lays the app out on a fixed 1920 x 1080 canvas and scales it to the screen, so every size in
 * the code is the pixel value from the design, whatever density the console reports.
 */
@Composable
fun DesignCanvas(content: @Composable () -> Unit) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.Ink),
        contentAlignment = Alignment.Center,
    ) {
        val scale = min(
            constraints.maxWidth.toFloat() / DESIGN_WIDTH,
            constraints.maxHeight.toFloat() / DESIGN_HEIGHT,
        )
        CompositionLocalProvider(LocalDensity provides Density(scale, 1f)) {
            Box(Modifier.size(DESIGN_WIDTH.dp, DESIGN_HEIGHT.dp)) {
                content()
            }
        }
    }
}

const val DESIGN_WIDTH = 1920f
const val DESIGN_HEIGHT = 1080f
