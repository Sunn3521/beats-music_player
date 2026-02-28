package com.sunn3521.vinylos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualizerSettingsScreen(
    settings: VisualizerSettings,
    onSettingsChange: (VisualizerSettings) -> Unit,
    onBack: () -> Unit,
    onActivateLockMode: () -> Unit
) {
    Scaffold(
        containerColor = Color(0xFF121212),
        topBar = {
            TopAppBar(
                title = { Text("Visualizer Studio", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Live Preview Area
            Text("Live Preview", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black)
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Generate dummy FFT data for preview
                var dummyFft by remember { mutableStateOf(List(64) { 0f }) }
                LaunchedEffect(Unit) {
                    while (true) {
                        dummyFft = List(64) { scala -> 
                            val noise = (Math.random() * 20).toFloat()
                            val base = if (scala % 8 == 0) 40f else 10f
                            (base + noise)
                        }
                        delay(50)
                    }
                }

                LockVisualizer(
                    fftData = dummyFft,
                    settings = settings,
                    modifier = Modifier.fillMaxSize(),
                    alpha = 1f
                )
                
                // Overlay text
                Box(modifier = Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.TopEnd) {
                    Text("PREVIEW", color = Color.White.copy(alpha = 0.2f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(24.dp))

            Text("Visualizer Type", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(VisualizerType.values()) { type ->
                    val isSelected = settings.type == type
                    Card(
                        onClick = { onSettingsChange(settings.copy(type = type)) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color.Red else Color(0xFF1E1E1E)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = type.name.replace("_", " "),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = Color.White,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Text("Rendering", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            ToggleSetting("Mirror Mode (Symmetric)", settings.mirrored) { onSettingsChange(settings.copy(mirrored = it)) }

            Spacer(Modifier.height(24.dp))

            Text("Appearance", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            
            SettingsSlider("Size", settings.visualizerSize, 0.5f..2.0f) { onSettingsChange(settings.copy(visualizerSize = it)) }
            SettingsSlider("Thickness", settings.thickness, 1f..40f) { onSettingsChange(settings.copy(thickness = it)) }
            SettingsSlider("Sensitivity", settings.sensitivity, 0.5f..3.0f) { onSettingsChange(settings.copy(sensitivity = it)) }
            SettingsSlider("Speed", settings.speed, 0.2f..2.0f) { onSettingsChange(settings.copy(speed = it)) }
            SettingsSlider("Transparency", settings.transparency, 0.1f..1.0f) { onSettingsChange(settings.copy(transparency = it)) }
            
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Glow Effect", color = Color.White, modifier = Modifier.weight(1f))
                Switch(checked = settings.glow, onCheckedChange = { onSettingsChange(settings.copy(glow = it)) })
            }
            if (settings.glow) {
                SettingsSlider("Glow Amount", settings.glowAmount, 0f..1f) { onSettingsChange(settings.copy(glowAmount = it)) }
            }

            Spacer(Modifier.height(24.dp))

            Text("Color Mode", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(AODColorMode.values()) { mode ->
                    val isSelected = settings.colorMode == mode
                    Card(
                        onClick = { onSettingsChange(settings.copy(colorMode = mode)) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color.Red else Color(0xFF1E1E1E)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = mode.name,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = Color.White
                        )
                    }
                }
            }
            
            if (settings.colorMode == AODColorMode.CUSTOM || settings.colorMode == AODColorMode.NEON) {
                Spacer(Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val colors = listOf(Color.Red, Color.Green, Color.Blue, Color.Yellow, Color.Cyan, Color.Magenta, Color.White)
                    items(colors) { color ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { onSettingsChange(settings.copy(color = color)) }
                                .border(2.dp, if (settings.color == color) Color.White else Color.Transparent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (settings.color == color) {
                                Icon(Icons.Default.Check, null, tint = if (color == Color.White) Color.Black else Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Text("Position", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            FlowRow(mainAxisSpacing = 8.dp, crossAxisSpacing = 8.dp) {
                AODPosition.values().forEach { pos ->
                    FilterChip(
                        selected = settings.visualizerPosition == pos,
                        onClick = { onSettingsChange(settings.copy(visualizerPosition = pos)) },
                        label = { Text(pos.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            labelColor = Color.White,
                            selectedLabelColor = Color.White,
                            selectedContainerColor = Color.Red
                        )
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text("System & AOD", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            ToggleSetting("Burn-in Protection (AMOLED)", settings.burnInProtection) { onSettingsChange(settings.copy(burnInProtection = it)) }
            ToggleSetting("Show Clock", settings.showClock) { onSettingsChange(settings.copy(showClock = it)) }
            ToggleSetting("Show Song Info", settings.showSongTitle) { onSettingsChange(settings.copy(showSongTitle = it)) }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = onActivateLockMode,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("ACTIVATE VISUALIZER MODE", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            
            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
fun SettingsSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
            Text("%.2f".format(value), color = Color.White, fontSize = 14.sp)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Color.Red,
                activeTrackColor = Color.Red,
                inactiveTrackColor = Color.White.copy(alpha = 0.1f)
            )
        )
    }
}

@Composable
fun ToggleSetting(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(label, color = Color.White, modifier = Modifier.weight(1f))
        Switch(
            checked = checked, 
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.Red, checkedTrackColor = Color.Red.copy(alpha = 0.5f))
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    mainAxisSpacing: androidx.compose.ui.unit.Dp,
    crossAxisSpacing: androidx.compose.ui.unit.Dp,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(mainAxisSpacing),
        verticalArrangement = Arrangement.spacedBy(crossAxisSpacing)
    ) {
        content()
    }
}
