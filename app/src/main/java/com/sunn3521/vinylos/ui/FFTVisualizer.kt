package com.sunn3521.vinylos.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun FFTVisualizer(
    magnitudes: List<Float>,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    mirrored: Boolean = true
) {
    val rawCount = magnitudes.size.coerceAtMost(64)
    if (rawCount == 0) {
        // Draw empty bars to keep layout stable and show something is there
        Canvas(modifier = modifier.fillMaxWidth().height(120.dp)) {
            val count = 32
            val widthStep = size.width / count
            val barWidth = widthStep * 0.7f
            for (i in 0 until count) {
                drawLine(
                    color = color.copy(alpha = 0.1f),
                    start = Offset(i * widthStep + (widthStep / 2), size.height),
                    end = Offset(i * widthStep + (widthStep / 2), size.height - 10f),
                    strokeWidth = barWidth,
                    cap = StrokeCap.Round
                )
            }
        }
        return
    }

    val displayMagnitudes = if (mirrored) {
        val half = magnitudes.take(rawCount / 2)
        half + half.reversed()
    } else {
        magnitudes.take(rawCount)
    }
    
    val barCount = displayMagnitudes.size
    val animatedMagnitudes = remember(barCount) { List(barCount) { Animatable(0.1f) } }

    LaunchedEffect(displayMagnitudes) {
        if (displayMagnitudes.isEmpty()) return@LaunchedEffect

        for (i in 0 until barCount) {
            val magnitude = displayMagnitudes[i]
            // Adjusted normalization for better visibility
            val normalizedHeight = (magnitude / 60f).coerceIn(0.05f, 1f)

            launch {
                animatedMagnitudes[i].animateTo(
                    targetValue = normalizedHeight,
                    animationSpec = tween(durationMillis = 150, easing = LinearEasing)
                )
            }
        }
    }

    Canvas(modifier = modifier.fillMaxWidth().height(120.dp)) {
        val widthStep = size.width / barCount
        val barWidth = widthStep * 0.7f

        for (i in 0 until barCount) {
            val barHeight = animatedMagnitudes[i].value * size.height

            drawLine(
                color = color,
                start = Offset(i * widthStep + (widthStep / 2), size.height),
                end = Offset(i * widthStep + (widthStep / 2), (size.height - barHeight).coerceAtMost(size.height - 5f)),
                strokeWidth = barWidth,
                cap = StrokeCap.Round
            )
        }
    }
}
