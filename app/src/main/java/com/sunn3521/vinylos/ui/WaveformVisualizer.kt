package com.sunn3521.vinylos.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import kotlin.random.Random

@Composable
fun WaveformVisualizer(
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    color: Color = Color.White
) {
    val barCount = 30
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    
    // Create random durations for each bar to make it look "organic"
    val durations = remember { List(barCount) { Random.nextInt(400, 800) } }
    
    val animations = durations.map { duration ->
        infiniteTransition.animateFloat(
            initialValue = 0.1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(duration, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "barHeight"
        )
    }

    Canvas(modifier = modifier.fillMaxWidth().height(40.dp)) {
        val widthStep = size.width / barCount
        val barWidth = 8f
        
        animations.forEachIndexed { i, animValue ->
            // If not playing, keep bars at a minimal "resting" height
            val heightFactor = if (isPlaying) animValue.value else 0.1f
            val barHeight = heightFactor * size.height
            
            drawLine(
                color = color,
                start = Offset(i * widthStep + (widthStep / 2), size.height / 2 + barHeight / 2),
                end = Offset(i * widthStep + (widthStep / 2), size.height / 2 - barHeight / 2),
                strokeWidth = barWidth,
                cap = StrokeCap.Round
            )
        }
    }
}
