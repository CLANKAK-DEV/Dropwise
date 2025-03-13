package com.example.dropwise

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.room.Room
import androidx.compose.ui.platform.LocalContext
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.CombinedData
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
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
    val prefs: SharedPreferences = context.getSharedPreferences("WaterGoals", Context.MODE_PRIVATE)
    val chartPrefs: SharedPreferences = context.getSharedPreferences("WaterIntakeChart", Context.MODE_PRIVATE)
    val db = remember {
        Room.databaseBuilder(context, AppDatabase::class.java, "dropwise_db")
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .build()
    }.apply { AppDatabase.INSTANCE = this }
    val userId = SessionManager.getUserId(context) ?: run {
        LaunchedEffect(Unit) {
            SnackbarHostState().showSnackbar("User not logged in. Please log in.")
        }
        return
    }
    var currentDate by remember { mutableStateOf(getTodayDate()) }
    var dailyIntake by remember { mutableStateOf(chartPrefs.getFloat("dailyIntake", 0f)) }
    var timeRangeData by remember { mutableStateOf<List<Pair<String, Float>>>(emptyList()) }
    var averageIntake by remember { mutableStateOf(0f) }
    var dailyWaterGoal by remember { mutableStateOf(prefs.getFloat("dailyWaterGoal", 0f)) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()
    var showChatDialog by remember { mutableStateOf(false) }
    var selectedTimeRange by remember { mutableStateOf("Week") }
    val updateTrigger by WaterIntakeUpdate.updateTrigger

    // Load drunk cups from SharedPreferences
    val drunkCups = remember { mutableStateMapOf<String, Boolean>().apply {
        val schedule = loadSchedule(prefs)
        schedule.forEach { (time, _) ->
            this[time] = prefs.getBoolean("drunk_$time", false)
            Log.d("DashboardScreen", "Loaded drunk status for $time: ${this[time]}")
        }
        Log.d("DashboardScreen", "Initial drunkCups: $this")
    } }

    // Register SharedPreferences listener for changes
    val prefsListener = remember {
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                "dailyIntake" -> {
                    dailyIntake = chartPrefs.getFloat("dailyIntake", 0f)
                    Log.d("DashboardScreen", "Daily intake updated from SharedPreferences: $dailyIntake")
                }
                "dailyWaterGoal" -> {
                    dailyWaterGoal = prefs.getFloat("dailyWaterGoal", 0f)
                    Log.d("DashboardScreen", "Daily water goal updated from SharedPreferences: $dailyWaterGoal")
                }
                else -> {
                    if (key != null && key.startsWith("drunk_")) {
                        val time = key.removePrefix("drunk_")
                        drunkCups[time] = prefs.getBoolean(key, false)
                        Log.d("DashboardScreen", "Drunk status updated for $time: ${drunkCups[time]}")
                        Log.d("DashboardScreen", "Current drunkCups: $drunkCups")
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        chartPrefs.registerOnSharedPreferenceChangeListener(prefsListener)
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
        onDispose {
            chartPrefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
            prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
        }
    }

    LaunchedEffect(userId, currentDate, selectedTimeRange, updateTrigger, dailyIntake, dailyWaterGoal, drunkCups.toMap()) {
        try {
            withContext(Dispatchers.IO) {
                // Fetch daily intake from database
                val dailyIntakeResult = db.userDao().getWaterIntakesForWeek(userId, currentDate, currentDate)
                val totalDailyIntakeFromDB = dailyIntakeResult.sumOf { it.amount?.toDouble() ?: 0.0 }.toFloat()

                // Process time range data
                val data = when (selectedTimeRange) {
                    "Day" -> {
                        // Debug: Fetch all water intakes for the day
                        val allIntakes = db.userDao().getAllWaterIntakesForDay(userId, currentDate)
                        Log.d("DashboardScreen", "All water intakes for $currentDate: $allIntakes")

                        // Debug: Log the drunkCups state
                        Log.d("DashboardScreen", "DrunkCups state: $drunkCups")

                        val hours = (7..23).toList() + (0..0).toList() // 7:00 to 23:00 + 0:00
                        hours.mapNotNull { hour ->
                            val scheduledTime = if (hour == 0) "0:00" else "$hour:00"
                            val isDrunk = drunkCups[scheduledTime] == true
                            Log.d("DashboardScreen", "Checking hour $hour ($scheduledTime), isDrunk: $isDrunk")
                            if (isDrunk) {
                                val total = db.userDao().getTotalWaterIntakeForHour(userId, currentDate, hour)?.toFloat() ?: 0f
                                Log.d("DashboardScreen", "Total intake for hour $hour from DB: $total")
                                // Fallback: Use SharedPreferences data (0.25L per drunk cup)
                                val totalFallback = if (isDrunk) 0.25f else 0f
                                Log.d("DashboardScreen", "Fallback total for hour $hour: $totalFallback")
                                val finalTotal = if (total > 0) total else totalFallback
                                Log.d("DashboardScreen", "Final total for hour $hour: $finalTotal")
                                if (finalTotal > 0) {
                                    scheduledTime to finalTotal
                                } else {
                                    null
                                }
                            } else {
                                null
                            }
                        }.also { data ->
                            Log.d("DashboardScreen", "Time range data for Day view: $data")
                        }
                    }
                    "Week" -> {
                        val startDate = getStartDate(currentDate)
                        val weekData = db.userDao().getWaterIntakesForWeek(userId, startDate, currentDate)
                        val calendar = Calendar.getInstance()
                        calendar.time = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(startDate)!!
                        (0..6).map { dayOffset ->
                            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
                            val total = weekData.filter { it.date == date }
                                .sumOf { it.amount?.toDouble() ?: 0.0 }.toFloat()
                            calendar.add(Calendar.DAY_OF_YEAR, 1)
                            date to total
                        }
                    }
                    "Month" -> {
                        val calendar = Calendar.getInstance()
                        calendar.time = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(currentDate)!!
                        calendar.set(Calendar.DAY_OF_MONTH, 1)
                        val start = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
                        calendar.add(Calendar.MONTH, 1)
                        calendar.add(Calendar.DAY_OF_MONTH, -1)
                        val end = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
                        val monthData = db.userDao().getWaterIntakesForRange(userId, start, end)

                        val weeksData = mutableListOf<Pair<String, Float>>()
                        calendar.time = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(start)!!
                        var weekNumber = 1
                        while (calendar.time <= SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(end)!!) {
                            val weekStart = calendar.time
                            calendar.add(Calendar.DAY_OF_YEAR, 6)
                            val weekEnd = if (calendar.time > SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(end)!!) {
                                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(end)!!
                            } else {
                                calendar.time
                            }
                            val weekRangeStart = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(weekStart)
                            val weekRangeEnd = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(weekEnd)
                            val weekTotal = monthData.filter { it.date >= weekRangeStart && it.date <= weekRangeEnd }
                                .sumOf { it.amount?.toDouble() ?: 0.0 }.toFloat()
                            weeksData.add("Week $weekNumber" to weekTotal)
                            weekNumber++
                            calendar.add(Calendar.DAY_OF_YEAR, 1)
                        }
                        weeksData
                    }
                    else -> emptyList()
                }

                withContext(Dispatchers.Main) {
                    dailyWaterGoal = prefs.getFloat("dailyWaterGoal", 0f)
                    val totalDailyIntake = maxOf(totalDailyIntakeFromDB, chartPrefs.getFloat("dailyIntake", 0f))
                    dailyIntake = totalDailyIntake
                    timeRangeData = data
                    val amounts = data.map { it.second }
                    // Calculate average intake per hour (total intake divided by number of hours)
                    averageIntake = if (data.isNotEmpty()) amounts.sum() / data.size else 0f
                    Log.d("DashboardScreen", "Updated UI: dailyIntake=$dailyIntake, dailyWaterGoal=$dailyWaterGoal, averageIntake=$averageIntake")
                }
            }
        } catch (e: Exception) {
            Log.e("DashboardScreen", "Error in LaunchedEffect: ${e.message}", e)
            scope.launch {
                snackbarHostState.showSnackbar("Error loading data: ${e.message ?: "Unknown error"}")
            }
        }
    }

    val dateRange = when (selectedTimeRange) {
        "Day" -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
        "Week" -> "${formatDisplayDate(getStartDate(currentDate))} - ${formatDisplayDate(currentDate)}"
        "Month" -> {
            val calendar = Calendar.getInstance()
            calendar.time = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(currentDate)!!
            "${calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())} ${calendar.get(Calendar.YEAR)}"
        }
        else -> ""
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = Color(0xFFF5F9FD)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = paddingValues.calculateTopPadding() + 24.dp,
                    bottom = paddingValues.calculateBottomPadding() + 24.dp
                )
                .verticalScroll(scrollState)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White, Color(0xFFF5F9FD))
                    )
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Move chatbot icon to the top




            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Current Intake",
                        fontSize = 20.sp,
                        color = Color(0xFF424242),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Text(
                        text = "${String.format(Locale.getDefault(), "%.1f", dailyIntake)}L",
                        fontSize = 30.sp,
                        color = Color(0xFF0288D1),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { selectedTimeRange = "Day" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedTimeRange == "Day") Color(0xFF0288D1) else Color.White,
                        contentColor = if (selectedTimeRange == "Day") Color.White else Color(0xFF0288D1)
                    ),
                    modifier = Modifier
                        .padding(4.dp)
                        .shadow(elevation = 0.dp, shape = RoundedCornerShape(8.dp))
                ) {
                    Text("Day", color = if (selectedTimeRange == "Day") Color.White else Color(0xFF0288D1), fontSize = 14.sp)
                }
                Button(
                    onClick = { selectedTimeRange = "Week" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedTimeRange == "Week") Color(0xFF0288D1) else Color.White,
                        contentColor = if (selectedTimeRange == "Week") Color.White else Color(0xFF0288D1)
                    ),
                    modifier = Modifier
                        .padding(4.dp)
                        .shadow(elevation = 0.dp, shape = RoundedCornerShape(8.dp))
                ) {
                    Text("Week", color = if (selectedTimeRange == "Week") Color.White else Color(0xFF0288D1), fontSize = 14.sp)
                }
                Button(
                    onClick = { selectedTimeRange = "Month" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedTimeRange == "Month") Color(0xFF0288D1) else Color.White,
                        contentColor = if (selectedTimeRange == "Month") Color.White else Color(0xFF0288D1)
                    ),
                    modifier = Modifier
                        .padding(4.dp)
                        .shadow(elevation = 0.dp, shape = RoundedCornerShape(8.dp))
                ) {
                    Text("Month", color = if (selectedTimeRange == "Month") Color.White else Color(0xFF0288D1), fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = dateRange,
                fontSize = 16.sp,
                color = Color(0xFF757575),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                textAlign = TextAlign.Center,
                fontStyle = MaterialTheme.typography.bodyMedium.fontStyle
            )

            Spacer(modifier = Modifier.height(28.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(horizontal = 16.dp)
                    .shadow(elevation = 10.dp, shape = RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    factory = { ctx ->
                        CombinedChart(ctx).apply {
                            description.text = "Water Intake (L)"
                            description.textSize = 8f
                            description.textColor = Color(0xFF757575).toArgb()
                            setDrawGridBackground(false)
                            setPinchZoom(true)
                            setBackgroundColor(android.graphics.Color.WHITE)
                            xAxis.apply {
                                valueFormatter = object : ValueFormatter() {
                                    override fun getFormattedValue(value: Float): String {
                                        val index = value.toInt()
                                        return when (selectedTimeRange) {
                                            "Day" -> {
                                                if (index in timeRangeData.indices) {
                                                    timeRangeData[index].first // Show 7:00, 8:00, ..., 23:00, 0:00
                                                } else {
                                                    ""
                                                }
                                            }
                                            "Week" -> {
                                                if (index in timeRangeData.indices) {
                                                    val date = timeRangeData[index].first
                                                    SimpleDateFormat("E", Locale.getDefault()).format(
                                                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date)!!
                                                    ).substring(0, 3)
                                                } else {
                                                    ""
                                                }
                                            }
                                            "Month" -> {
                                                if (index in timeRangeData.indices) {
                                                    timeRangeData[index].first // "Week 1", "Week 2", etc.
                                                } else {
                                                    ""
                                                }
                                            }
                                            else -> ""
                                        }
                                    }
                                }
                                position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                                textColor = Color(0xFF757575).toArgb()
                                textSize = 6f
                                granularity = 1f
                                setDrawGridLines(true)
                                gridColor = Color(0xFFD3E3F1).toArgb()
                                gridLineWidth = 0.3f
                                labelRotationAngle = -45f
                                axisMinimum = -0.5f
                                axisMaximum = when (selectedTimeRange) {
                                    "Day" -> timeRangeData.size.toFloat() - 0.5f
                                    "Week" -> 6.5f
                                    "Month" -> timeRangeData.size.toFloat() - 0.5f
                                    else -> 0f
                                }
                                labelCount = when (selectedTimeRange) {
                                    "Day" -> timeRangeData.size
                                    "Week" -> 7
                                    "Month" -> timeRangeData.size
                                    else -> 10
                                }
                            }
                            axisLeft.apply {
                                valueFormatter = object : ValueFormatter() {
                                    override fun getFormattedValue(value: Float): String {
                                        return "${value}L"
                                    }
                                }
                                axisMinimum = 0f
                                axisMaximum = 6f
                                textColor = Color(0xFF757575).toArgb()
                                textSize = 6f
                                setDrawGridLines(true)
                                gridColor = Color(0xFFD3E3F1).toArgb()
                                gridLineWidth = 0.3f
                                enableGridDashedLine(8f, 8f, 0f)
                            }
                            axisRight.isEnabled = false
                            legend.apply {
                                isEnabled = true
                                textColor = Color(0xFF424242).toArgb()
                                textSize = 6f
                                setDrawInside(false)
                                form = com.github.mikephil.charting.components.Legend.LegendForm.CIRCLE
                                formSize = 4f
                                xEntrySpace = 6f
                                yEntrySpace = 3f
                            }
                            animateY(800, com.github.mikephil.charting.animation.Easing.EaseInOutCubic)
                        }
                    },
                    update = { chart ->
                        try {
                            val barEntries = timeRangeData.mapIndexed { index, (_, amount) ->
                                BarEntry(index.toFloat(), amount)
                            }
                            val goalEntries = when (selectedTimeRange) {
                                "Day" -> (0 until timeRangeData.size).map { Entry(it.toFloat(), dailyWaterGoal / 24f) }
                                "Week" -> (0 until 7).map { Entry(it.toFloat(), dailyWaterGoal) }
                                "Month" -> (0 until timeRangeData.size).map { Entry(it.toFloat(), dailyWaterGoal) }
                                else -> emptyList()
                            }

                            val barDataSet = BarDataSet(barEntries, "Water Intake (L)").apply {
                                color = Color(0xFF42A5F5).toArgb()
                                setDrawValues(true)
                                valueTextColor = Color(0xFF333333).toArgb()
                                valueTextSize = 5f
                                valueFormatter = object : ValueFormatter() {
                                    override fun getFormattedValue(value: Float): String {
                                        return if (value > 0) String.format(Locale.getDefault(), "%.1fL", value) else ""
                                    }
                                }
                                barShadowColor = Color(0x20000000).toArgb()
                                highLightColor = Color(0xFFFFCA28).toArgb()
                            }

                            val lineDataSet = LineDataSet(goalEntries, "Daily Goal (L)").apply {
                                color = Color(0xFF66BB6A).toArgb()
                                setDrawValues(false)
                                lineWidth = 1f
                                setDrawCircles(true)
                                circleRadius = 1.5f
                                setCircleColor(Color(0xFF66BB6A).toArgb())
                                enableDashedLine(6f, 6f, 0f)
                            }

                            val barData = BarData(barDataSet).apply { barWidth = 0.2f }
                            val lineData = LineData(lineDataSet)
                            val combinedData = CombinedData().apply {
                                setData(barData)
                                setData(lineData)
                            }

                            chart.data = combinedData
                            chart.invalidate()
                        } catch (e: Exception) {
                            Log.e("DashboardScreen", "Error updating chart: ${e.message}", e)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Average ${selectedTimeRange.lowercase()} Intake",
                        fontSize = 20.sp,
                        color = Color(0xFF424242),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Text(
                        text = "${String.format(Locale.getDefault(), "%.1f", averageIntake)}L",
                        fontSize = 24.sp,
                        color = Color(0xFF0288D1),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier
                    .size(220.dp)
                    .padding(16.dp)
                    .shadow(elevation = 10.dp, shape = RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { (dailyIntake / dailyWaterGoal).coerceIn(0f, 1f) },
                        modifier = Modifier.size(200.dp),
                        color = Color(0xFF42A5F5),
                        strokeWidth = 20.dp,
                        strokeCap = StrokeCap.Round
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${String.format(Locale.getDefault(), "%.1f", dailyIntake)}L",
                            fontSize = 28.sp,
                            color = Color(0xFF0288D1),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "/ ${String.format(Locale.getDefault(), "%.1f", dailyWaterGoal)}L",
                            fontSize = 18.sp,
                            color = Color(0xFF757575),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
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

fun getStartDate(currentDate: String): String {
    val calendar = Calendar.getInstance()
    calendar.time = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(currentDate)!!
    calendar.add(Calendar.DAY_OF_YEAR, -6)
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
}

private fun loadSchedule(prefs: SharedPreferences): List<Pair<String, Float>> {
    val size = prefs.getInt("schedule_size", 0)
    return (0 until size).map { i ->
        val time = prefs.getString("schedule_time_$i", "00:00") ?: "00:00"
        val amount = prefs.getFloat("schedule_amount_$i", 0f)
        time to amount
    }
}