package com.sunn3521.vinylos.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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

enum class AODPosition {
    TOP, CENTER, BOTTOM, LEFT, RIGHT, CUSTOM, FULL_SCREEN
}

enum class AODColorMode {
    AUTO, WHITE, BLUE, NEON, CUSTOM, GRADIENT, RGB
}

data class VisualizerSettings(
    val type: VisualizerType = VisualizerType.BAR,
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
        else -> 48
    }
    
    var displayMagnitudes by remember(barCount) { mutableStateOf(List(barCount) { 0.1f }) }

    LaunchedEffect(fftData, barCount, settings.sensitivity) {
        val currentFft = fftData
        if (currentFft.isNotEmpty()) {
            val newList = List(barCount) { i ->
                val raw = if (i < currentFft.size) currentFft[i] else 0f
                (raw / 60f * settings.sensitivity).coerceIn(0.1f, 1f)
            }
            displayMagnitudes = newList
        }
        delay(16)
    }

    Canvas(
        modifier = modifier.then(if (settings.visualizerPosition == AODPosition.FULL_SCREEN) Modifier.fillMaxSize() else Modifier.fillMaxWidth().height(200.dp))
    ) {
        val drawAlpha = alpha * settings.transparency
        
        val magnitudes = if (settings.mirrored && settings.type != VisualizerType.CIRCULAR) {
            val half = displayMagnitudes.take(displayMagnitudes.size / 2)
            half + half.reversed()
        } else {
            displayMagnitudes
        }

        when (settings.type) {
            VisualizerType.BAR -> drawVisualizerBars(magnitudes, settings, drawAlpha)
            VisualizerType.CIRCULAR -> drawCircularVisualizer(magnitudes, settings, drawAlpha)
            VisualizerType.EDGE -> drawEdgeVisualizer(magnitudes, settings, drawAlpha)
            VisualizerType.WAVE -> drawWaveVisualizer(magnitudes, settings, drawAlpha)
            VisualizerType.PARTICLES -> drawParticlesVisualizer(magnitudes, settings, drawAlpha)
        }
    }
}

private fun DrawScope.drawVisualizerBars(magnitudes: List<Float>, settings: VisualizerSettings, alpha: Float) {
    if (magnitudes.isEmpty()) return

    val totalWidth = size.width
    val count = magnitudes.size
    val barWidthWithGap = totalWidth / count
    val barWidth = barWidthWithGap * 0.8f

    magnitudes.forEachIndexed { index, magnitude ->
        val x = index * barWidthWithGap + (barWidthWithGap / 2)
        val barHeight = magnitude * size.height * 0.5f * settings.visualizerSize
        
        drawLine(
            color = settings.color.copy(alpha = alpha),
            start = Offset(x, size.height / 2),
            end = Offset(x, size.height / 2 - barHeight),
            strokeWidth = barWidth,
            cap = StrokeCap.Round
        )
        
        drawLine(
            color = settings.color.copy(alpha = alpha * 0.5f),
            start = Offset(x, size.height / 2),
            end = Offset(x, size.height / 2 + barHeight),
            strokeWidth = barWidth,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawCircularVisualizer(magnitudes: List<Float>, settings: VisualizerSettings, alpha: Float, rgbOffset: Float = 0f) {
    val radius = (size.minDimension / 4f) * settings.visualizerSize
    val count = magnitudes.size
    
    val centerX = size.width/2 + settings.offsetX
    val centerY = size.height/2 + settings.offsetY

    magnitudes.forEachIndexed { index, magnitude ->
        val angle = (360f / count) * index
        val length = magnitude * size.minDimension * 0.25f * settings.visualizerSize
        
        val angleRad = Math.toRadians(angle.toDouble())
        val cosVal = cos(angleRad).toFloat()
        val sinVal = sin(angleRad).toFloat()
        
        val color = if (settings.colorMode == AODColorMode.RGB) {
            Color.hsv((rgbOffset + (index.toFloat() / count * 360f)) % 360f, 1f, 1f)
        } else settings.color
        
        val start = Offset(
            centerX + radius * cosVal,
            centerY + radius * sinVal
        )
        val end = Offset(
            centerX + (radius + length) * cosVal,
            centerY + (radius + length) * sinVal
        )
        
        if (settings.glow || settings.colorMode == AODColorMode.NEON) {
            drawLine(
                color = color.copy(alpha = 0.3f * alpha * (if(settings.colorMode == AODColorMode.NEON) 0.8f else settings.glowAmount)),
                start = start,
                end = Offset(
                    centerX + (radius + length + 15f) * cosVal,
                    centerY + (radius + length + 15f) * sinVal
                ),
                strokeWidth = settings.thickness + 12f,
                cap = StrokeCap.Round
            )
        }
        
        drawLine(
            color = color.copy(alpha = alpha),
            start = start,
            end = end,
            strokeWidth = settings.thickness,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawEdgeVisualizer(magnitudes: List<Float>, settings: VisualizerSettings, alpha: Float) {
    val count = magnitudes.size
    val barHeightWithGap = size.height / count
    val barHeight = barHeightWithGap * 0.8f
    
    magnitudes.forEachIndexed { index, magnitude ->
        val y = index * barHeightWithGap + (barHeightWithGap / 2)
        val barWidth = magnitude * size.width * 0.2f * settings.visualizerSize
        
        drawLine(
            color = settings.color.copy(alpha = alpha),
            start = Offset(0f, y),
            end = Offset(barWidth, y),
            strokeWidth = barHeight,
            cap = StrokeCap.Round
        )
        
        drawLine(
            color = settings.color.copy(alpha = alpha),
            start = Offset(size.width, y),
            end = Offset(size.width - barWidth, y),
            strokeWidth = barHeight,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawWaveVisualizer(magnitudes: List<Float>, settings: VisualizerSettings, alpha: Float) {
    if (magnitudes.isEmpty()) return
    
    val path = Path()
    val widthStep = size.width / (magnitudes.size - 1)
    
    magnitudes.forEachIndexed { index, magnitude ->
        val x = index * widthStep
        val y = size.height / 2 + (magnitude * size.height * 0.2f * settings.visualizerSize * if (index % 2 == 0) 1 else -1)
        
        if (index == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    
    drawPath(
        path = path,
        color = settings.color.copy(alpha = alpha),
        style = Stroke(width = settings.thickness)
    )
}

private fun DrawScope.drawParticlesVisualizer(magnitudes: List<Float>, settings: VisualizerSettings, alpha: Float) {
    val centerX = size.width / 2
    val centerY = size.height / 2
    
    magnitudes.forEachIndexed { index, magnitude ->
        val angle = (360f / magnitudes.size) * index
        val distance = (100f + magnitude * 200f) * settings.visualizerSize
        
        val angleRad = Math.toRadians(angle.toDouble())
        val x = centerX + distance * cos(angleRad).toFloat()
        val y = centerY + distance * sin(angleRad).toFloat()
        
        drawCircle(
            color = settings.color.copy(alpha = alpha),
            radius = (magnitude * settings.thickness).coerceAtLeast(2f),
            center = Offset(x, y)
        )
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
    
    Text(
        text = formatter.format(currentTime),
        color = Color.White.copy(alpha = 0.9f * alpha),
        fontSize = settings.clockSize.sp,
        fontWeight = FontWeight.Thin,
        modifier = modifier
    )
}

@Composable
fun AODSongTitle(modifier: Modifier = Modifier, title: String, artist: String, settings: VisualizerSettings, alpha: Float) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = Color.White.copy(alpha = 0.8f * alpha),
            fontSize = settings.titleSize.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = artist,
            color = Color.White.copy(alpha = 0.5f * alpha),
            fontSize = (settings.titleSize * 0.7f).sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun LockModeScreen(
    musicService: MusicService,
    settings: VisualizerSettings,
    onSettingsChange: (VisualizerSettings) -> Unit,
    onExit: () -> Unit
) {
    val song = musicService.currentSong ?: return
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

    val dimAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(1500),
        label = "dimAlpha"
    )

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)
        .pointerInput(Unit) {
            detectTapGestures(
                onDoubleTap = { 
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    onExit() 
                }
            )
        }
        .offset { burnInOffset }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            if (settings.showClock) {
                AODClock(
                    settings = settings,
                    alpha = dimAlpha,
                    modifier = Modifier.padding(top = 64.dp)
                )
            } else {
                Spacer(Modifier.height(1.dp))
            }

            LockVisualizer(
                fftData = musicService.fftMagnitudes,
                settings = settings,
                alpha = dimAlpha,
                modifier = Modifier.fillMaxWidth().height(300.dp)
            )

            if (settings.showSongTitle) {
                AODSongTitle(
                    title = song.title,
                    artist = song.artist,
                    settings = settings,
                    alpha = dimAlpha,
                    modifier = Modifier.padding(bottom = 64.dp)
                )
            } else {
                Spacer(Modifier.height(1.dp))
            }
        }
    }
}
