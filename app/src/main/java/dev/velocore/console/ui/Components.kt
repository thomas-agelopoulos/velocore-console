package dev.velocore.console.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun T(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    align: TextAlign? = null,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = if (align != null) style.copy(textAlign = align) else style,
        overflow = TextOverflow.Ellipsis,
        maxLines = maxLines,
    )
}

/** Tap target with a gentle shrink while pressed, no ripple. */
fun Modifier.pressable(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "press")
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick)
}

fun Modifier.card(color: Color = Palette.Surface1, radius: Int = 32): Modifier =
    this
        .clip(RoundedCornerShape(radius.dp))
        .background(color)

/** Frosted panel used over photos. */
fun Modifier.glass(radius: Int, color: Color = Palette.GlassDark, edge: Boolean = true): Modifier {
    val shape = RoundedCornerShape(radius.dp)
    val base = this
        .clip(shape)
        .background(color)
    return if (edge) base.border(1.dp, Palette.GlassEdge, shape) else base
}

@Composable
fun Cover(
    @DrawableRes image: Int,
    modifier: Modifier = Modifier,
    verticalBias: Float = 0f,
) {
    Image(
        painter = painterResource(image),
        contentDescription = null,
        modifier = modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
        alignment = androidx.compose.ui.BiasAlignment(0f, verticalBias),
    )
}

/** White pill, black label: the primary action everywhere. */
@Composable
fun PrimaryPill(
    label: String,
    modifier: Modifier = Modifier,
    height: Int = 88,
    fontSize: Int = 26,
    icon: ImageVector? = Icons.Play,
    iconSize: Int = 26,
    onClick: () -> Unit,
) {
    Pill(label, modifier, height, fontSize, icon, iconSize, Palette.Text, Palette.Ink, onClick)
}

@Composable
fun SecondaryPill(
    label: String,
    modifier: Modifier = Modifier,
    height: Int = 84,
    fontSize: Int = 22,
    icon: ImageVector? = null,
    iconSize: Int = 24,
    onClick: () -> Unit,
) {
    Pill(label, modifier, height, fontSize, icon, iconSize, Palette.Surface2, Palette.Text, onClick)
}

@Composable
private fun Pill(
    label: String,
    modifier: Modifier,
    height: Int,
    fontSize: Int,
    icon: ImageVector?,
    iconSize: Int,
    background: Color,
    foreground: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(height.dp)
            .pressable(onClick = onClick)
            .clip(RoundedCornerShape((height / 2).dp))
            .background(background)
            .padding(horizontal = 36.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) Icon(icon, iconSize, foreground)
            T(label, ts(fontSize, 600, foreground), maxLines = 1)
        }
    }
}

@Composable
fun GlassChip(text: String, height: Int = 40, fontSize: Int = 18, color: Color = Palette.Text, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(height.dp)
            .glass(height / 2, Palette.Glass, edge = false)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        T(text, ts(fontSize, 600, color), maxLines = 1)
    }
}

@Composable
fun Scrim(modifier: Modifier, brush: androidx.compose.ui.graphics.Brush) {
    Box(modifier.background(brush))
}

@Composable
fun BoxScope.BottomScrim(fraction: Float, alpha: Float, to: Color = Color.Black) {
    Box(
        Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .fillMaxHeight(fraction)
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(to.copy(alpha = 0f), to.copy(alpha = alpha)),
                ),
            ),
    )
}
