package se.pettersoft.steplord.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import se.pettersoft.steplord.ui.theme.StepBlue
import se.pettersoft.steplord.ui.theme.StepBlueDeep

private val RingWidth = 14.dp
private val RingGap = 14.dp

/**
 * Large round button with a blue gradient fill, surrounded by a ring showing [progress] (0..1).
 * The button is only clickable when [onClick] is non-null.
 */
@Composable
fun StepCircle(
    progress: Float,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    /** What a click does, read out by screen readers, e.g. "Change daily goal". */
    onClickLabel: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800),
        label = "progress",
    )
    val ringColor = MaterialTheme.colorScheme.primary
    val trackColor = ringColor.copy(alpha = 0.15f)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .drawBehind {
                val stroke = RingWidth.toPx()
                val arcTopLeft = Offset(stroke / 2, stroke / 2)
                val arcSize = Size(size.width - stroke, size.height - stroke)
                drawArc(trackColor, 0f, 360f, false, arcTopLeft, arcSize, style = Stroke(stroke))
                if (animatedProgress > 0f) {
                    drawArc(
                        color = ringColor,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
            }
            .padding(RingWidth + RingGap),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = Color.Transparent,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxSize(),
        ) {
            CompositionLocalProvider(LocalContentColor provides Color.White) {
                // Clickable inside the Surface, so the ripple is clipped to the circle without
                // clipping the Surface's shadow
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(listOf(StepBlue, StepBlueDeep)))
                        .clickable(enabled = onClick != null, onClickLabel = onClickLabel) { onClick?.invoke() },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    content = content,
                )
            }
        }
    }
}
