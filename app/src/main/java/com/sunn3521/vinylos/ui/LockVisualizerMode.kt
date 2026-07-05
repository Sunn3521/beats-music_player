package com.sunn3521.vinylos.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sunn3521.vinylos.data.model.Song
import com.sunn3521.vinylos.player.MusicService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

enum class VisualizerType {
    BAR, CIRCULAR, EDGE, WAVE, PARTICLES
}

enum class VisualizerOrientation {
    HORIZONTAL, VERTICAL
}

enum class AODPosition {
    TOP, CENTER, BOTTOM, LEFT, RIGHT, CUSTOM, FULL_SCREEN
}

enum class AODColorMode {
    AUTO, WHITE, BLUE, NEON, CUSTOM, GRADIENT, RGB
}

data class VisualizerSettings(
    val type: VisualizerType = VisualizerType.BAR,
    val orientation: VisualizerOrientation = VisualizerOrientation.HORIZONTAL,
    val sensitivity: Float = 1f,
    val thickness: Float = 14f,
    val color: Color = Color.Red,
    val glow: Boolean = true,
    val glowAmount: Float = 0.5f,
    val transparency: Float = 1.0f,
    val speed: Float = 1.0f,
    val isAmoled: Boolean = true,
    
    // AOD Layout
    val showClock: Boolean = true,
    val showSongTitle: Boolean = true,
    val clockPosition: AODPosition = AODPosition.TOP,
    val titlePosition: AODPosition = AODPosition.BOTTOM,
    val visualizerPosition: AODPosition = AODPosition.CENTER,
    val clockSize: Float = 80f,
    val titleSize: Float = 24f,
    val visualizerSize: Float = 1f,
    val colorMode: AODColorMode = AODColorMode.AUTO,
    
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    
    val aodEnabled: Boolean = true,
    val onlyWhenCharging: Boolean = false,
    val disableLowBattery: Boolean = true,
    
    val burnInProtection: Boolean = true,
    val mirrored: Boolean = true
)

fun mapAODPosition(position: AODPosition): Alignment {
    return when (position) {
        AODPosition.TOP -> Alignment.TopCenter
        AODPosition.CENTER -> Alignment.Center
        AODPosition.BOTTOM -> Alignment.BottomCenter
        AODPosition.LEFT -> Alignment.CenterStart
        AODPosition.RIGHT -> Alignment.CenterEnd
        AODPosition.CUSTOM -> Alignment.Center
        AODPosition.FULL_SCREEN -> Alignment.Center
    }
}

@Composable
fun LockVisualizer(
    fftData: List<Float>,
    modifier: Modifier = Modifier,
    settings: VisualizerSettings = VisualizerSettings(),
    alpha: Float = 1f
) {
    val barCount = when(settings.type) {
        VisualizerType.CIRCULAR -> 64
        VisualizerType.EDGE -> 120
        else -> 48
    }
    
    var displayMagnitudes by remember(barCount) { mutableStateOf(List(barCount) { 0.1f }) }

    LaunchedEffect(fftData, barCount, settings.sensitivity) {
        if (fftData.isNotEmpty()) {
            val newList = List(barCount) { i ->
                val fftIndex = (i.toFloat() / barCount * fftData.size).toInt().coerceIn(0, fftData.size - 1)
                val raw = fftData[fftIndex]
                (raw / 60f * settings.sensitivity).coerceIn(0.1f, 1f)
            }
            displayMagnitudes = newList
        }
        delay(16)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "visualizer")
    val rgbOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rgbOffset"
    )

    val isFullScreen = settings.visualizerPosition == AODPosition.FULL_SCREEN || settings.type == VisualizerType.EDGE
    Canvas(
        modifier = modifier.then(if (isFullScreen) Modifier.fillMaxSize() else Modifier.fillMaxWidth().height(200.dp))
    ) {
        val drawAlpha = alpha * settings.transparency
        
        val magnitudes = if (settings.mirrored && settings.type != VisualizerType.CIRCULAR) {
            if (settings.type == VisualizerType.EDGE) {
                val halfSize = displayMagnitudes.size / 2
                val half = displayMagnitudes.take(halfSize)
                half + half
            } else {
                val half = displayMagnitudes.take(displayMagnitudes.size / 2)
                half + half.reversed()
            }
        } else {
            displayMagnitudes
        }

        when (settings.type) {
            VisualizerType.BAR -> drawVisualizerBars(magnitudes, settings, drawAlpha, rgbOffset)
            VisualizerType.CIRCULAR -> drawCircularVisualizer(magnitudes, settings, drawAlpha, rgbOffset)
            VisualizerType.EDGE -> drawEdgeVisualizer(magnitudes, settings, drawAlpha, rgbOffset)
            VisualizerType.WAVE -> drawWaveVisualizer(magnitudes, settings, drawAlpha, rgbOffset)
            VisualizerType.PARTICLES -> drawParticlesVisualizer(magnitudes, settings, drawAlpha, rgbOffset)
        }
    }
}

private fun getVisualizerColor(index: Int, count: Int, settings: VisualizerSettings, rgbOffset: Float): Color {
    val effectiveIndex = if (settings.type == VisualizerType.EDGE) index % (count / 2).coerceAtLeast(1) else index
    val effectiveCount = if (settings.type == VisualizerType.EDGE) count / 2 else count

    return when (settings.colorMode) {
        AODColorMode.RGB -> Color.hsv((rgbOffset + (effectiveIndex.toFloat() / effectiveCount.coerceAtLeast(1) * 360f)) % 360f, 1f, 1f)
        AODColorMode.GRADIENT -> {
            val fraction = effectiveIndex.toFloat() / effectiveCount.coerceAtLeast(1)
            Color.hsv((180f + fraction * 120f) % 360f, 0.75f, 1f)
        }
        AODColorMode.NEON -> settings.color
        AODColorMode.WHITE -> Color.White
        AODColorMode.BLUE -> Color(0xFF00E5FF)
        else -> settings.color
    }
}

private fun DrawScope.drawVisualizerBars(magnitudes: List<Float>, settings: VisualizerSettings, alpha: Float, rgbOffset: Float) {
    if (magnitudes.isEmpty()) return

    val totalWidth = size.width
    val totalHeight = size.height
    val count = magnitudes.size
    val isNeon = settings.colorMode == AODColorMode.NEON
    val isVertical = settings.orientation == VisualizerOrientation.VERTICAL

    magnitudes.forEachIndexed { index, magnitude ->
        val color = getVisualizerColor(index, count, settings, rgbOffset)
        
        if (isVertical) {
            val barHeightWithGap = totalHeight / count
            val barHeight = barHeightWithGap * 0.8f
            val y = index * barHeightWithGap + (barHeightWithGap / 2)
            val barWidth = magnitude * totalWidth * 0.5f * settings.visualizerSize
            
            if (settings.glow || isNeon) {
                drawLine(
                    color = color.copy(alpha = 0.3f * alpha * settings.glowAmount),
                    start = Offset(totalWidth / 2 - barWidth - 10f, y),
                    end = Offset(totalWidth / 2 + barWidth + 10f, y),
                    strokeWidth = barHeight + 8f,
                    cap = StrokeCap.Round
                )
            }
            drawLine(color.copy(alpha = alpha), Offset(totalWidth / 2, y), Offset(totalWidth / 2 - barWidth, y), barHeight, StrokeCap.Round)
            drawLine(color.copy(alpha = alpha * 0.5f), Offset(totalWidth / 2, y), Offset(totalWidth / 2 + barWidth, y), barHeight, StrokeCap.Round)
        } else {
            val barWidthWithGap = totalWidth / count
            val barWidth = barWidthWithGap * 0.8f
            val x = index * barWidthWithGap + (barWidthWithGap / 2)
            val barHeight = magnitude * totalHeight * 0.5f * settings.visualizerSize
            
            if (settings.glow || isNeon) {
                drawLine(
                    color = color.copy(alpha = 0.3f * alpha * settings.glowAmount),
                    start = Offset(x, totalHeight / 2 - barHeight - 10f),
                    end = Offset(x, totalHeight / 2 + barHeight + 10f),
                    strokeWidth = barWidth + 8f,
                    cap = StrokeCap.Round
                )
            }
            drawLine(color.copy(alpha = alpha), Offset(x, totalHeight / 2), Offset(x, totalHeight / 2 - barHeight), barWidth, StrokeCap.Round)
            drawLine(color.copy(alpha = alpha * 0.5f), Offset(x, totalHeight / 2), Offset(x, totalHeight / 2 + barHeight), barWidth, StrokeCap.Round)
        }
    }
}

private fun DrawScope.drawCircularVisualizer(magnitudes: List<Float>, settings: VisualizerSettings, alpha: Float, rgbOffset: Float) {
    val radius = (size.minDimension / 4f) * settings.visualizerSize
    val count = magnitudes.size
    val centerX = size.width / 2 + settings.offsetX
    val centerY = size.height / 2 + settings.offsetY
    val isNeon = settings.colorMode == AODColorMode.NEON

    magnitudes.forEachIndexed { index, magnitude ->
        val angle = (360f / count) * index
        val length = magnitude * size.minDimension * 0.25f * settings.visualizerSize
        val angleRad = Math.toRadians(angle.toDouble())
        val cosVal = cos(angleRad).toFloat()
        val sinVal = sin(angleRad).toFloat()
        val color = getVisualizerColor(index, count, settings, rgbOffset)
        
        val start = Offset(centerX + radius * cosVal, centerY + radius * sinVal)
        val end = Offset(centerX + (radius + length) * cosVal, centerY + (radius + length) * sinVal)
        
        if (settings.glow || isNeon) {
            drawLine(
                color = color.copy(alpha = 0.3f * alpha * settings.glowAmount),
                start = start,
                end = Offset(centerX + (radius + length + 15f) * cosVal, centerY + (radius + length + 15f) * sinVal),
                strokeWidth = settings.thickness + 12f,
                cap = StrokeCap.Round
            )
        }
        drawLine(color.copy(alpha = alpha), start, end, settings.thickness, StrokeCap.Round)
    }
}

private fun DrawScope.drawEdgeVisualizer(magnitudes: List<Float>, settings: VisualizerSettings, alpha: Float, rgbOffset: Float) {
    val countPerSide = magnitudes.size / 2
    val isVertical = settings.orientation == VisualizerOrientation.VERTICAL
    val isNeon = settings.colorMode == AODColorMode.NEON
    
    magnitudes.forEachIndexed { index, magnitude ->
        val side = index / countPerSide.coerceAtLeast(1)
        val posInSide = index % countPerSide.coerceAtLeast(1)
        val color = getVisualizerColor(index, magnitudes.size, settings, rgbOffset)
        
        if (isVertical) { // Top and Bottom Edges
            val barWidthWithGap = size.width / countPerSide.coerceAtLeast(1)
            val barWidth = barWidthWithGap * 0.9f
            val x = posInSide * barWidthWithGap + (barWidthWithGap / 2)
            val barHeight = magnitude * size.height * 0.25f * settings.visualizerSize
            
            if (side == 0) { // Top
                if (settings.glow || isNeon) {
                    drawLine(color.copy(alpha = 0.4f * alpha * settings.glowAmount), Offset(x, 0f), Offset(x, barHeight + 12f), barWidth + 6f, StrokeCap.Round)
                }
                drawLine(color.copy(alpha = alpha), Offset(x, 0f), Offset(x, barHeight), barWidth, StrokeCap.Round)
            } else { // Bottom
                if (settings.glow || isNeon) {
                    drawLine(color.copy(alpha = 0.4f * alpha * settings.glowAmount), Offset(x, size.height), Offset(x, size.height - barHeight - 12f), barWidth + 6f, StrokeCap.Round)
                }
                drawLine(color.copy(alpha = alpha), Offset(x, size.height), Offset(x, size.height - barHeight), barWidth, StrokeCap.Round)
            }
        } else { // Left and Right Edges
            val barHeightWithGap = size.height / countPerSide.coerceAtLeast(1)
            val barHeight = barHeightWithGap * 0.9f
            val y = posInSide * barHeightWithGap + (barHeightWithGap / 2)
            val barWidth = magnitude * size.width * 0.25f * settings.visualizerSize
            
            if (side == 0) { // Left
                if (settings.glow || isNeon) {
                    drawLine(color.copy(alpha = 0.4f * alpha * settings.glowAmount), Offset(0f, y), Offset(barWidth + 12f, y), barHeight + 6f, StrokeCap.Round)
                }
                drawLine(color.copy(alpha = alpha), Offset(0f, y), Offset(barWidth, y), barHeight, StrokeCap.Round)
            } else { // Right
                if (settings.glow || isNeon) {
                    drawLine(color.copy(alpha = 0.4f * alpha * settings.glowAmount), Offset(size.width, y), Offset(size.width - barWidth - 12f, y), barHeight + 6f, StrokeCap.Round)
                }
                drawLine(color.copy(alpha = alpha), Offset(size.width, y), Offset(size.width - barWidth, y), barHeight, StrokeCap.Round)
            }
        }
    }
}

private fun DrawScope.drawWaveVisualizer(magnitudes: List<Float>, settings: VisualizerSettings, alpha: Float, rgbOffset: Float) {
    if (magnitudes.isEmpty()) return
    val path = Path()
    val isVertical = settings.orientation == VisualizerOrientation.VERTICAL
    val color = getVisualizerColor(0, magnitudes.size, settings, rgbOffset)
    
    if (isVertical) {
        val heightStep = size.height / (magnitudes.size - 1).coerceAtLeast(1)
        magnitudes.forEachIndexed { index, magnitude ->
            val y = index * heightStep
            val x = size.width / 2 + (magnitude * size.width * 0.2f * settings.visualizerSize * if (index % 2 == 0) 1 else -1)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
    } else {
        val widthStep = size.width / (magnitudes.size - 1).coerceAtLeast(1)
        magnitudes.forEachIndexed { index, magnitude ->
            val x = index * widthStep
            val y = size.height / 2 + (magnitude * size.height * 0.2f * settings.visualizerSize * if (index % 2 == 0) 1 else -1)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
    }
    
    if (settings.glow) {
        drawPath(path, color.copy(alpha = 0.3f * alpha * settings.glowAmount), style = Stroke(width = settings.thickness + 10f, cap = StrokeCap.Round))
    }
    drawPath(path, color.copy(alpha = alpha), style = Stroke(width = settings.thickness, cap = StrokeCap.Round))
}

private fun DrawScope.drawParticlesVisualizer(magnitudes: List<Float>, settings: VisualizerSettings, alpha: Float, rgbOffset: Float) {
    val centerX = size.width / 2
    val centerY = size.height / 2
    magnitudes.forEachIndexed { index, magnitude ->
        val angle = (360f / magnitudes.size.coerceAtLeast(1)) * index
        val distance = (100f + magnitude * 200f) * settings.visualizerSize
        val color = getVisualizerColor(index, magnitudes.size, settings, rgbOffset)
        val angleRad = Math.toRadians(angle.toDouble())
        val x = centerX + distance * cos(angleRad).toFloat()
        val y = centerY + distance * sin(angleRad).toFloat()
        
        if (settings.glow) {
            drawCircle(color.copy(alpha = 0.4f * alpha * settings.glowAmount), (magnitude * settings.thickness + 8f).coerceAtLeast(4f), Offset(x, y))
        }
        drawCircle(color.copy(alpha = alpha), (magnitude * settings.thickness).coerceAtLeast(2f), Offset(x, y))
    }
}

@Composable
fun AODClock(modifier: Modifier = Modifier, settings: VisualizerSettings, alpha: Float) {
    var currentTime by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Date()
            delay(60_000)
        }
    }
    val formatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    Text(formatter.format(currentTime), color = Color.White.copy(alpha = 0.9f * alpha), fontSize = settings.clockSize.sp, fontWeight = FontWeight.Thin, modifier = modifier)
}

@Composable
fun AODSongTitle(modifier: Modifier = Modifier, title: String, artist: String, settings: VisualizerSettings, alpha: Float) {
    Column(modifier = modifier.padding(horizontal = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = Color.White.copy(alpha = 0.8f * alpha), fontSize = settings.titleSize.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(artist, color = Color.White.copy(alpha = 0.5f * alpha), fontSize = (settings.titleSize * 0.7f).sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun LockModeScreen(
    musicService: MusicService,
    settings: VisualizerSettings,
    onSettingsChange: (VisualizerSettings) -> Unit,
    onExit: () -> Unit
) {
    val currentSong = musicService.currentSong ?: return
    val view = LocalView.current
    var burnInOffset by remember { mutableStateOf(IntOffset(0, 0)) }
    LaunchedEffect(settings.burnInProtection) {
        if (settings.burnInProtection) {
            while (true) {
                delay(20000)
                burnInOffset = IntOffset(Random.nextInt(-3, 4), Random.nextInt(-3, 4))
            }
        } else {
            burnInOffset = IntOffset(0, 0)
        }
    }
    val dimAlpha by animateFloatAsState(targetValue = 1f, animationSpec = tween(1500), label = "dimAlpha")

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)
        .draggable(
            orientation = Orientation.Horizontal,
            state = rememberDraggableState { _ -> },
            onDragStopped = { velocity ->
                if (velocity > 1000) { musicService.previous(); view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) }
                else if (velocity < -1000) { musicService.next(); view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) }
            }
        )
        .pointerInput(Unit) {
            detectTapGestures(onDoubleTap = { view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS); onExit() })
        }
        .offset { burnInOffset }
    ) {
        LockVisualizer(fftData = musicService.fftMagnitudes, settings = settings, alpha = dimAlpha, modifier = Modifier.fillMaxSize())
        AnimatedContent(
            targetState = currentSong,
            transitionSpec = {
                (fadeIn() + slideInHorizontally { width -> width }).togetherWith(fadeOut() + slideOutHorizontally { width -> -width })
            },
            label = "song_transition"
        ) { song ->
            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
                if (settings.showClock) AODClock(settings = settings, alpha = dimAlpha, modifier = Modifier.padding(top = 64.dp))
                else Spacer(Modifier.height(1.dp))
                Spacer(Modifier.weight(1f))
                if (settings.showSongTitle) AODSongTitle(title = song.title, artist = song.artist, settings = settings, alpha = dimAlpha, modifier = Modifier.padding(bottom = 64.dp))
                else Spacer(Modifier.height(1.dp))
            }
        }
    }
}
