package com.example.dropwise

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GoalsScreen(activity: ComponentActivity) {
    var goal by remember { mutableStateOf("2") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Objectifs",
            fontSize = 24.sp,
            color = Color(0xFF333333),
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = goal,
            onValueChange = { goal = it },
            label = { Text("Daily Goal (L)", color = Color(0xFF333333)) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        LinearProgressIndicator(
            progress = { 0.75f },
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF7ED321),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text("75% de l'objectif atteint", color = Color(0xFF333333))
    }
}