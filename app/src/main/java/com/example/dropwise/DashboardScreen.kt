package com.example.dropwise

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.room.Room
import androidx.compose.ui.platform.LocalContext
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

fun getTodayDate(): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
}

@Composable
fun DashboardScreen() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    // Use a singleton database instance
    val db = remember {
        Room.databaseBuilder(context, AppDatabase::class.java, "dropwise_db")
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .fallbackToDestructiveMigration() // For development; remove in production if migrations are needed
            .build()
    }.apply { AppDatabase.INSTANCE = this }
    val userId = SessionManager.getUserId(context) ?: run {
        LaunchedEffect(Unit) {
            SnackbarHostState().showSnackbar("User not logged in. Please log in.")
        }
        return
    }
    var currentDate by remember { mutableStateOf(getTodayDate()) }
    var dailyIntake by remember { mutableStateOf(0f) }
    var weeklyIntakes by remember { mutableStateOf<List<WaterIntake>>(emptyList()) }
    var averageIntake by remember { mutableStateOf(0f) }
    var showDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    fun getStartDate(currentDate: String): String {
        val calendar = Calendar.getInstance()
        calendar.time = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(currentDate)!!
        calendar.add(Calendar.DAY_OF_YEAR, -6)
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
    }

    fun updateDate(currentDate: String, days: Int, scope: CoroutineScope, onUpdate: (String) -> Unit) {
        scope.launch {
            val calendar = Calendar.getInstance()
            calendar.time = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(currentDate)!!
            calendar.add(Calendar.DAY_OF_YEAR, days)
            onUpdate(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time))
        }
    }

    fun formatDisplayDate(date: String): String =
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date)!!
        )

    LaunchedEffect(userId, currentDate) {
        try {
            withContext(Dispatchers.IO) {
                val intake = db.userDao().getWaterIntakeForDate(userId, currentDate)
                val startDate = getStartDate(currentDate)
                val weekly = db.userDao().getWaterIntakesForWeek(userId, startDate, currentDate)
                withContext(Dispatchers.Main) {
                    dailyIntake = (intake?.amount?.toFloat() ?: 0f)
                    weeklyIntakes = weekly
                    val amounts = weekly.mapNotNull { it.amount?.toFloat() } // Handle nulls and convert to Float
                    averageIntake = if (amounts.isNotEmpty()) amounts.average().toFloat() else 0f
                }
            }
        } catch (e: Exception) {
            Log.e("DashboardScreen", "Failed to fetch data: ${e.message}", e)
            scope.launch {
                snackbarHostState.showSnackbar("Failed to load data: ${e.message ?: "Unknown error"}")
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(
                    top = paddingValues.calculateTopPadding() + 16.dp,
                    bottom = paddingValues.calculateBottomPadding() + 16.dp,
                    start = 16.dp,
                    end = 16.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Tableau de bord",
                fontSize = 28.sp,
                color = Color(0xFF333333),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Date Navigation
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = {
                    updateDate(currentDate, -1, scope) { newDate -> currentDate = newDate }
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous Day", tint = Color(0xFF4A90E2))
                }
                Text(
                    text = formatDisplayDate(currentDate),
                    fontSize = 18.sp,
                    color = Color(0xFF333333),
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
                IconButton(onClick = {
                    updateDate(currentDate, 1, scope) { newDate -> currentDate = newDate }
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next Day", tint = Color(0xFF4A90E2))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Weekly Consumption Bar Chart (MPAndroidChart)
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .background(Color.White)
                    .padding(8.dp),
                factory = { ctx ->
                    BarChart(ctx).apply {
                        description.isEnabled = false
                        setDrawGridBackground(false)
                        xAxis.apply {
                            valueFormatter = object : ValueFormatter() {
                                override fun getFormattedValue(value: Float): String {
                                    val index = value.toInt()
                                    return if (index in weeklyIntakes.indices) {
                                        formatDisplayDate(weeklyIntakes[index].date).substring(0, 5)
                                    } else ""
                                }
                            }
                            textColor = Color(0xFF333333).toArgb()
                            textSize = 12f
                            setDrawGridLines(false)
                        }
                        axisLeft.apply {
                            axisMinimum = 0f
                            axisMaximum = 4f // Adjust based on your max intake
                            textColor = Color(0xFF333333).toArgb()
                            textSize = 12f
                        }
                        axisRight.isEnabled = false
                        legend.isEnabled = false
                        animateY(1000)
                    }
                },
                update = { chart ->
                    val entries = weeklyIntakes.mapIndexedNotNull { index, intake ->
                        intake.amount?.toFloat()?.let { BarEntry(index.toFloat(), it) }
                    }
                    if (entries.isNotEmpty()) {
                        val dataSet = BarDataSet(entries, "Weekly Intake").apply {
                            colors = weeklyIntakes.mapNotNull {
                                it.amount?.toFloat()?.let { amount ->
                                    if (amount >= 2.0f) Color(0xFF7ED321).toArgb() else Color(0xFF4A90E2).toArgb()
                                }
                            }
                            valueTextColor = Color(0xFF333333).toArgb()
                            valueTextSize = 12f
                        }
                        chart.data = BarData(dataSet).apply { barWidth = 0.5f }
                    } else {
                        chart.data = BarData() // Empty data to avoid crash
                    }
                    chart.invalidate()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Average Consumption Display
            Text(
                text = "Moyenne hebdomadaire : ${String.format(Locale.getDefault(), "%.1f", averageIntake)}L",
                fontSize = 16.sp,
                color = Color(0xFF333333),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Daily Consumption Circular Progress (Compose Native)
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { (dailyIntake / 3f).coerceIn(0f, 1f) },
                    modifier = Modifier.size(150.dp),
                    color = Color(0xFF4A90E2),
                    strokeWidth = 20.dp
                )
                Text(
                    text = "${String.format(Locale.getDefault(), "%.1f", dailyIntake)}L",
                    fontSize = 24.sp,
                    color = Color(0xFF333333),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Goal Progress Gauge (Simple Compose Implementation)
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { (averageIntake / 3f).coerceIn(0f, 1f) },
                    modifier = Modifier.size(200.dp),
                    color = when {
                        averageIntake >= 2.25f -> Color.Blue
                        averageIntake >= 1.5f -> Color.Green
                        averageIntake >= 0.75f -> Color.Yellow
                        else -> Color.Red
                    },
                    strokeWidth = 20.dp
                )
                Text(
                    text = "${String.format(Locale.getDefault(), "%.0f", averageIntake * 100 / 3)}%",
                    fontSize = 24.sp,
                    color = Color(0xFF333333),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Spacer(modifier = Modifier.weight(1f))

            // Floating Action Button
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = Color(0xFF4A90E2),
                contentColor = Color.White,
                modifier = Modifier.align(Alignment.End).size(64.dp).padding(bottom = 16.dp)
            ) {
                Icon(Icons.Default.Add, "Add Water", tint = Color.White)
            }

            // Dialog for Adding Water
            if (showDialog) {
                var amount by remember { mutableStateOf("") }
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text("Ajouter de l'eau", color = Color(0xFF333333)) },
                    text = {
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { amount = it.filter { char -> char.isDigit() || char == '.' } },
                            label = { Text("Quantité (L)", color = Color(0xFF333333)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            isError = amount.toFloatOrNull()?.let { it <= 0 } ?: true,
                            supportingText = {
                                if (amount.toFloatOrNull()?.let { it <= 0 } ?: true) {
                                    Text("Enter a valid amount > 0", color = Color.Red)
                                }
                            }
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            val waterAmount = amount.toFloatOrNull()
                            if (waterAmount != null && waterAmount > 0) {
                                scope.launch {
                                    try {
                                        withContext(Dispatchers.IO) {
                                            db.userDao().insertWaterIntake(
                                                WaterIntake(userId = userId, amount = waterAmount.toDouble(), date = currentDate)
                                            )
                                        }
                                        dailyIntake += waterAmount
                                        showDialog = false
                                        snackbarHostState.showSnackbar("Water added successfully!")
                                        // Refresh data
                                        withContext(Dispatchers.IO) {
                                            val intake = db.userDao().getWaterIntakeForDate(userId, currentDate)
                                            val startDate = getStartDate(currentDate)
                                            val weekly = db.userDao().getWaterIntakesForWeek(userId, startDate, currentDate)
                                            withContext(Dispatchers.Main) {
                                                dailyIntake = (intake?.amount?.toFloat() ?: 0f)
                                                weeklyIntakes = weekly
                                                val amounts = weekly.mapNotNull { it.amount?.toFloat() }
                                                averageIntake = if (amounts.isNotEmpty()) amounts.average().toFloat() else 0f
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e("DashboardScreen", "Failed to add water: ${e.message}", e)
                                        snackbarHostState.showSnackbar("Failed to add water: ${e.message ?: "Unknown error"}")
                                    }
                                }
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Invalid amount")
                                }
                            }
                        }) {
                            Text("Confirmer", color = Color(0xFF4A90E2))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDialog = false }) {
                            Text("Annuler", color = Color(0xFF333333))
                        }
                    }
                )
            }
        }
    }
}