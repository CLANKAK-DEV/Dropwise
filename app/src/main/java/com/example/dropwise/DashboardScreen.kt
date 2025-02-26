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
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry

@Composable
fun DashboardScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Tableau de bord",
            fontSize = 20.sp,
            color = Color(0xFF333333)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Vous avez bu : 1.5L aujourd'hui",
            fontSize = 16.sp,
            color = Color(0xFF333333)
        )
        Spacer(modifier = Modifier.height(16.dp))
        AndroidView(
            factory = { context ->
                BarChart(context).apply {
                    val entries = listOf(
                        BarEntry(1f, 1.5f), BarEntry(2f, 1.8f), BarEntry(3f, 1.2f),
                        BarEntry(4f, 2.0f), BarEntry(5f, 1.7f), BarEntry(6f, 1.9f), BarEntry(7f, 1.4f)
                    )
                    val dataSet = BarDataSet(entries, "Water Intake (L)")
                    dataSet.colors = listOf(
                        Color(0xFF4A90E2).toArgb(),
                        Color(0xFF7ED321).toArgb()
                    )
                    val barData = BarData(dataSet)
                    this.data = barData
                    invalidate()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )
        Spacer(modifier = Modifier.weight(1f))
        FloatingActionButton(
            onClick = { /* Add water intake logic */ },
            containerColor = Color(0xFF4A90E2),
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(text = "+", color = Color.White, fontSize = 20.sp)
        }
    }
}