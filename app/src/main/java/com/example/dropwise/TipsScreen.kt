package com.example.dropwise

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.launch

@Composable
fun TipsScreen() {
    val scope = rememberCoroutineScope()
    var tip by remember { mutableStateOf("Buvez un verre d'eau avant chaque repas.") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Conseils d'hydratation",
            fontSize = 20.sp,
            color = Color(0xFF333333)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Text(
                text = tip,
                color = Color(0xFF333333),
                modifier = Modifier.padding(8.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                scope.launch {
                    val model = GenerativeModel("gemini-1.5-pro", BuildConfig.API_KEY)
                    val prompt = "Provide a creative tip to stay hydrated during the day."
                    tip = model.generateContent(prompt)?.text ?: tip
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90E2))
        ) {
            Text(text = "Refresh Tip", color = Color.White)
        }
    }
}