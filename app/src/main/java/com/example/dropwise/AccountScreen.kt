package com.example.dropwise

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AccountScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Mon compte",
            fontSize = 20.sp,
            color = Color(0xFF333333)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text(text = "Nom: User", color = Color(0xFF333333))
                Text(text = "Objectif: 2L", color = Color(0xFF333333))
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = { /* Logout logic */ },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90E2)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Déconnexion", color = Color.White, fontSize = 16.sp)
        }
    }
}