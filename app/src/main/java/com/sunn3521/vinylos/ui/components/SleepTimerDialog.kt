package com.sunn3521.vinylos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun SleepTimerDialog(
    onDismiss: () -> Unit,
    onStart: (Long, Boolean) -> Unit
) {
    var hours by remember { mutableIntStateOf(0) }
    var minutes by remember { mutableIntStateOf(30) }
    var seconds by remember { mutableIntStateOf(0) }
    var finishCurrentSong by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Sleep Timer",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TimeDial("Hours", hours, 0..23) { hours = it }
                    TimeDial("Mins", minutes, 0..59) { minutes = it }
                    TimeDial("Secs", seconds, 0..59) { seconds = it }
                }
                
                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = finishCurrentSong,
                        onCheckedChange = { finishCurrentSong = it },
                        colors = CheckboxDefaults.colors(checkedColor = Color.Red, uncheckedColor = Color.Gray)
                    )
                    Text(
                        "Finish being played song",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
                
                Spacer(Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCEL", color = Color.White.copy(alpha = 0.6f))
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val millis = (hours * 3600 + minutes * 60 + seconds) * 1000L
                            onStart(millis, finishCurrentSong)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("START", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun TimeDial(label: String, value: Int, range: IntRange, onValueChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.Gray, fontSize = 12.sp)
        IconButton(onClick = { if (value < range.last) onValueChange(value + 1) else onValueChange(range.first) }) {
            Text("▲", color = Color.Red)
        }
        Text(
            "%02d".format(value),
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = { if (value > range.first) onValueChange(value - 1) else onValueChange(range.last) }) {
            Text("▼", color = Color.Red)
        }
    }
}
