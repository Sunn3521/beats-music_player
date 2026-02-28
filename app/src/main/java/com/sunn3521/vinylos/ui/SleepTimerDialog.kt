package com.sunn3521.vinylos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun SleepTimerDialog(
    onDismiss: () -> Unit,
    onStart: (Long) -> Unit
) {
    var hours by remember { mutableIntStateOf(0) }
    var minutes by remember { mutableIntStateOf(30) }
    var seconds by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        titleContentColor = Color.White,
        textContentColor = Color.Gray,
        shape = RoundedCornerShape(28.dp),
        title = { 
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Sleep Timer", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().height(160.dp)
                ) {
                    TimePickerWheel(
                        range = 0..23,
                        initialValue = hours,
                        onValueChange = { hours = it },
                        label = "hr"
                    )
                    Text(":", color = Color.White.copy(alpha = 0.5f), fontSize = 28.sp, fontWeight = FontWeight.Light, modifier = Modifier.padding(bottom = 20.dp))
                    TimePickerWheel(
                        range = 0..59,
                        initialValue = minutes,
                        onValueChange = { minutes = it },
                        label = "min"
                    )
                    Text(":", color = Color.White.copy(alpha = 0.5f), fontSize = 28.sp, fontWeight = FontWeight.Light, modifier = Modifier.padding(bottom = 20.dp))
                    TimePickerWheel(
                        range = 0..59,
                        initialValue = seconds,
                        onValueChange = { seconds = it },
                        label = "sec"
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val totalMillis = (hours * 3600L + minutes * 60L + seconds) * 1000L
                    if (totalMillis > 0) onStart(totalMillis)
                    else onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text("Start", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.7f))
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun TimePickerWheel(
    range: IntRange,
    initialValue: Int,
    onValueChange: (Int) -> Unit,
    label: String
) {
    val items = range.toList()
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialValue)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    
    val itemHeight = 45.dp
    
    LaunchedEffect(listState.firstVisibleItemIndex) {
        onValueChange(items[listState.firstVisibleItemIndex])
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(70.dp)
    ) {
        Box(
            modifier = Modifier
                .height(itemHeight * 3)
                .width(50.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                    .border(1.dp, Color.Red.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            )
            
            LazyColumn(
                state = listState,
                flingBehavior = flingBehavior,
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(vertical = itemHeight)
            ) {
                items(items.size) { index ->
                    val isSelected by remember { derivedStateOf { listState.firstVisibleItemIndex == index } }
                    Box(
                        modifier = Modifier.height(itemHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = String.format(Locale.US, "%02d", items[index]),
                            color = if (isSelected) Color.Red else Color.White.copy(alpha = 0.3f),
                            fontSize = if (isSelected) 24.sp else 18.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(text = label, color = Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
