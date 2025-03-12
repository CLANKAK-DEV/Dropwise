package com.example.dropwise

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.airbnb.lottie.compose.*
import java.util.Calendar
import kotlin.math.roundToInt
import android.content.SharedPreferences
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush

class WaterReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = NotificationManagerCompat.from(context)
        val channelId = "water_reminder_channel"
        val notificationId = intent.getIntExtra("notificationId", 0)
        val message = intent.getStringExtra("message") ?: "Time to Drink Water!"

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.dropwiselogo)
            .setContentTitle("Water Reminder")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(notificationId, builder.build())
        }
    }
}

@Composable
fun GoalsScreen(activity: ComponentActivity) {
    val context = LocalContext.current
    val prefs: SharedPreferences = context.getSharedPreferences("WaterGoals", Context.MODE_PRIVATE)
    var inputValue by remember { mutableStateOf(prefs.getString("inputValue", "") ?: "") }
    var inputType by remember { mutableStateOf(prefs.getString("inputType", "Weight") ?: "Weight") }
    var dailyWaterGoal by remember { mutableStateOf(prefs.getFloat("dailyWaterGoal", 0f)) }
    var waterPerHour by remember { mutableStateOf(prefs.getFloat("waterPerHour", 0f)) }
    var cupsPerDay by remember { mutableStateOf(prefs.getInt("cupsPerDay", 0)) }
    var progress by remember { mutableStateOf(prefs.getFloat("progress", 0f)) }
    var schedule by remember { mutableStateOf(loadSchedule(prefs)) }
    val drunkCups = remember { mutableStateMapOf<String, Boolean>().apply {
        schedule.forEach { (time, _) -> this[time] = prefs.getBoolean("drunk_$time", false) }
    } }
    var globalAlarmActive by remember { mutableStateOf(prefs.getBoolean("globalAlarmActive", true)) }

    // Check and trigger congratulation notification when progress reaches 100%
    LaunchedEffect(progress) {
        if (progress >= 1.0f && globalAlarmActive) {
            sendCongratulationNotification(context)
        }
    }

    LaunchedEffect(Unit) {
        createNotificationChannel(context)
        if (globalAlarmActive) {
            scheduleNotifications(context, schedule, drunkCups)
        }
        val loadedSchedule = loadSchedule(prefs)
        Log.d("GoalsScreen", "Initial schedule loaded from SharedPreferences: $loadedSchedule")
    }

    val scrollState = rememberScrollState()

    val nextDrinkTime = schedule
        .filter { (time, _) -> !drunkCups.getOrDefault(time, false) }
        .minByOrNull { (time, _) ->
            val (hour, minute) = time.split(":").map { it.toIntOrNull() ?: 0 }
            hour * 60 + minute
        }?.first ?: "No scheduled time"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(Color(0xFFF5F5F5))
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Objectifs",
            fontSize = 28.sp,
            color = Color(0xFF1A73E8),
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = inputType == "Weight",
                    onClick = {
                        inputType = "Weight"
                        Log.d("GoalsScreen", "Selected input type: Weight")
                        updateGoals(inputValue, inputType, prefs, drunkCups, { schedule = it }, {
                            dailyWaterGoal = it
                            waterPerHour = it / 16f
                            cupsPerDay = (it / 0.25f).roundToInt().coerceAtLeast(1)
                            progress = 0f
                        }, globalAlarmActive, context)
                    },
                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF1A73E8))
                )
                Text("Weight (kg)", color = Color(0xFF333333))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = inputType == "Liters",
                    onClick = {
                        inputType = "Liters"
                        Log.d("GoalsScreen", "Selected input type: Liters")
                        updateGoals(inputValue, inputType, prefs, drunkCups, { schedule = it }, {
                            dailyWaterGoal = it
                            waterPerHour = it / 16f
                            cupsPerDay = (it / 0.25f).roundToInt().coerceAtLeast(1)
                            progress = 0f
                        }, globalAlarmActive, context)
                    },
                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF1A73E8))
                )
                Text("Daily Goal (L)", color = Color(0xFF333333))
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = inputValue,
            onValueChange = { newValue ->
                Log.d("GoalsScreen", "Input changed to: $newValue")
                val validInput = newValue.filter { it.isDigit() || it == '.' }
                inputValue = if (validInput.count { it == '.' } <= 1) validInput else inputValue
                updateGoals(
                    inputValue = inputValue,
                    inputType = inputType,
                    prefs = prefs,
                    drunkCups = drunkCups,
                    onScheduleUpdate = { newSchedule ->
                        Log.d("GoalsScreen", "Schedule updated: $newSchedule")
                        schedule = newSchedule
                        if (globalAlarmActive) {
                            scheduleNotifications(context, schedule, drunkCups)
                        }
                    },
                    onGoalUpdate = { newDailyGoal ->
                        Log.d("GoalsScreen", "Goal updated: $newDailyGoal")
                        dailyWaterGoal = newDailyGoal
                        waterPerHour = newDailyGoal / 16f
                        cupsPerDay = (newDailyGoal / 0.25f).roundToInt().coerceAtLeast(1)
                        progress = 0f
                    },
                    globalAlarmActive = globalAlarmActive,
                    context = context
                )
            },
            label = { Text(if (inputType == "Weight") "Weight (kg)" else "Daily Goal (L)", color = Color(0xFF333333)) },
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.height(20.dp))

        if (dailyWaterGoal > 0) {
            Text(
                text = String.format("Daily Water Goal: %.2f L", dailyWaterGoal),
                fontSize = 18.sp,
                color = Color(0xFF333333)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = String.format("Water per Hour: %.2f L", waterPerHour),
                fontSize = 16.sp,
                color = Color(0xFF333333)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Cups per Day (250ml): $cupsPerDay",
                fontSize = 16.sp,
                color = Color(0xFF333333)
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Custom Progress Bar with Water Animation
            CustomWaterProgressBar(progress = progress)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${(progress * 100).toInt()}% de l'objectif atteint",
                color = Color(0xFF333333),
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(20.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text("Enable Notifications", fontSize = 16.sp, color = Color(0xFF333333))
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = globalAlarmActive,
                    onCheckedChange = { newValue ->
                        globalAlarmActive = newValue
                        with(prefs.edit()) {
                            putBoolean("globalAlarmActive", globalAlarmActive)
                            apply()
                        }
                        if (globalAlarmActive) {
                            scheduleNotifications(context, schedule, drunkCups)
                        } else {
                            schedule.forEachIndexed { index, _ ->
                                cancelNotification(context, schedule[index].first, index)
                            }
                        }
                    },
                    colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF7ED321))
                )
            }
            Spacer(modifier = Modifier.height(20.dp))

            Column {
                schedule.chunked(3).forEach { rowCups ->
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rowCups.forEach { (time, amount) ->
                            CupItem(
                                time = time,
                                amount = amount,
                                isDrunk = drunkCups[time] ?: false,
                                isNextDrink = time == nextDrinkTime,
                                onCupClicked = {
                                    if (!(drunkCups[time] ?: false)) {
                                        drunkCups[time] = true
                                        val newProgress = (drunkCups.count { it.value } * 0.25f) / dailyWaterGoal
                                        progress = newProgress.coerceAtMost(1f)
                                        with(prefs.edit()) {
                                            putBoolean("drunk_$time", true)
                                            putFloat("progress", progress)
                                            apply()
                                        }
                                        if (globalAlarmActive) {
                                            cancelNotification(context, time, schedule.indexOfFirst { it.first == time })
                                            scheduleNotifications(context, schedule, drunkCups)
                                        }
                                    }
                                },
                                onTimeEdited = { newTime ->
                                    val oldEntry = schedule.find { it.first == time }
                                    if (oldEntry != null) {
                                        val updatedSchedule = schedule.map { entry ->
                                            if (entry.first == time) newTime to entry.second
                                            else entry
                                        }.sortedBy { it.first }

                                        // Save the updated schedule with indexed keys
                                        val editor = prefs.edit()
                                        updatedSchedule.forEachIndexed { index, (time, amount) ->
                                            editor.putString("schedule_time_$index", time)
                                            editor.putFloat("schedule_amount_$index", amount)
                                            editor.putBoolean("drunk_$time", drunkCups[time] ?: false)
                                        }
                                        editor.putInt("schedule_size", updatedSchedule.size)
                                        val success = editor.commit() // Synchronous save
                                        if (success) {
                                            schedule = updatedSchedule // Update state after successful save
                                            val wasDrunk = drunkCups[time] ?: false
                                            drunkCups.remove(time)
                                            drunkCups[newTime] = wasDrunk
                                            if (globalAlarmActive) {
                                                schedule.forEachIndexed { index, _ ->
                                                    cancelNotification(context, schedule[index].first, index)
                                                }
                                                scheduleNotifications(context, updatedSchedule, drunkCups)
                                            }
                                            Log.d("GoalsScreen", "Time changed successfully: $time to $newTime, saved schedule: $updatedSchedule")
                                        } else {
                                            Log.e("GoalsScreen", "Failed to save time change: $time to $newTime")
                                        }
                                    }
                                }
                            )
                        }
                        repeat(3 - rowCups.size) {
                            Spacer(modifier = Modifier.width(100.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun CupItem(
    time: String,
    amount: Float,
    isDrunk: Boolean,
    onCupClicked: () -> Unit,
    onTimeEdited: (String) -> Unit,
    isNextDrink: Boolean = false
) {
    val currentTime = Calendar.getInstance()
    val (hour, minute) = time.split(":").map { it.toInt() }
    val isApproaching = currentTime.get(Calendar.HOUR_OF_DAY) == hour && currentTime.get(Calendar.MINUTE) in (minute - 10)..(minute - 1)
    val isCurrentTime = currentTime.get(Calendar.HOUR_OF_DAY) == hour && currentTime.get(Calendar.MINUTE) in (minute - 5)..minute
    val isPastTime = currentTime.get(Calendar.HOUR_OF_DAY) > hour ||
            (currentTime.get(Calendar.HOUR_OF_DAY) == hour && currentTime.get(Calendar.MINUTE) > minute)

    val shakeAnim = rememberInfiniteTransition()
    val offsetX by shakeAnim.animateFloat(
        initialValue = 0f,
        targetValue = if (isApproaching) 8f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(150, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val scaleApproaching by shakeAnim.animateFloat(
        initialValue = 1f,
        targetValue = if (isApproaching) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val pulseAnim = rememberInfiniteTransition()
    val scalePulse by pulseAnim.animateFloat(
        initialValue = 1f,
        targetValue = if (isNextDrink && !isApproaching && !isCurrentTime && !isDrunk) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val finalScale = if (isApproaching) scaleApproaching else scalePulse

    val showTimePicker = remember { mutableStateOf(false) }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .offset(x = offsetX.dp)
            .scale(finalScale)
            .size(100.dp, 140.dp)
            .clickable(
                enabled = (isCurrentTime || isPastTime) && !isDrunk,
                onClick = onCupClicked
            )
            .alpha(if (isDrunk) 0.3f else 1f),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.cup),
                contentDescription = "Cup Image",
                modifier = Modifier
                    .size(50.dp)
                    .alpha(if (isDrunk) 0.3f else 1f)
            )

            Text(
                text = "$time\n${String.format("%.2f", amount)} L",
                color = Color(0xFF333333),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit Time",
                tint = Color(0xFF1A73E8),
                modifier = Modifier
                    .size(20.dp)
                    .clickable { showTimePicker.value = true }
            )
        }
    }

    if (showTimePicker.value) {
        val timePickerDialog = TimePickerDialog(
            context,
            { _, selectedHour, selectedMinute ->
                val newTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                onTimeEdited(newTime)
                showTimePicker.value = false
                Log.d("CupItem", "Time changed to: $newTime")
            },
            hour,
            minute,
            true
        )

        timePickerDialog.show()
    }
}

private fun updateGoals(
    inputValue: String,
    inputType: String,
    prefs: SharedPreferences,
    drunkCups: MutableMap<String, Boolean>,
    onScheduleUpdate: (List<Pair<String, Float>>) -> Unit,
    onGoalUpdate: (Float) -> Unit,
    globalAlarmActive: Boolean,
    context: Context
) {
    try {
        Log.d("GoalsScreen", "Updating goals with inputValue: $inputValue, inputType: $inputType")
        val newDailyGoal: Float = if (inputValue.isNotEmpty()) {
            when (inputType) {
                "Weight" -> (inputValue.toFloatOrNull() ?: 0f) * 0.033f
                "Liters" -> inputValue.toFloatOrNull() ?: 0f
                else -> 0f
            }
        } else {
            0f
        }
        Log.d("GoalsScreen", "Calculated newDailyGoal: $newDailyGoal")

        if (newDailyGoal > 0) {
            onGoalUpdate(newDailyGoal)
            drunkCups.clear()
            val cupsPerDay = (newDailyGoal / 0.25f).roundToInt().coerceAtLeast(1)
            val cupsInterval = 16f / cupsPerDay
            val newSchedule = (0 until cupsPerDay).map { i ->
                val hour = (7 + (i * cupsInterval)).toInt().coerceIn(0, 23)
                val minute = ((i * cupsInterval) % 1 * 60).toInt().coerceIn(0, 59)
                String.format("%02d:%02d", hour, minute) to 0.25f
            }
            Log.d("GoalsScreen", "New cupsPerDay: $cupsPerDay, cupsInterval: $cupsInterval")

            with(prefs.edit()) {
                putString("inputValue", inputValue)
                putString("inputType", inputType)
                putFloat("dailyWaterGoal", newDailyGoal)
                putFloat("waterPerHour", newDailyGoal / 16f)
                putInt("cupsPerDay", cupsPerDay)
                putFloat("progress", 0f)
                newSchedule.forEachIndexed { index, (time, amount) ->
                    putString("schedule_time_$index", time)
                    putFloat("schedule_amount_$index", amount)
                    putBoolean("drunk_$time", false)
                }
                putInt("schedule_size", newSchedule.size)
                putBoolean("globalAlarmActive", globalAlarmActive)
                apply()
            }
            onScheduleUpdate(newSchedule)
        } else {
            Log.d("GoalsScreen", "Daily goal is 0, no updates applied")
        }
    } catch (e: Exception) {
        Log.e("GoalsScreen", "Error updating goals", e)
    }
}

@Composable
fun CustomWaterProgressBar(progress: Float) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
    )

    // Water wave animation
    val waterComposition by rememberLottieComposition(
        LottieCompositionSpec.Url("https://lottie.host/8b8f3b73-7f5e-4f6b-8e8b-8f8e7e5f6b8e/WaterWaveAnimation.json")
    )
    val waterProgress by animateLottieCompositionAsState(
        composition = waterComposition,
        iterations = LottieConstants.IterateForever,
        speed = 1.5f
    )

    // Shake animation when progress reaches 100%
    val shakeAnim = rememberInfiniteTransition()
    val shakeOffsetX by shakeAnim.animateFloat(
        initialValue = 0f,
        targetValue = if (progress >= 1f) 6f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(120, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Scale animation when progress reaches 100%
    val scaleAnim = rememberInfiniteTransition()
    val scale by scaleAnim.animateFloat(
        initialValue = 1f,
        targetValue = if (progress >= 1f) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp)
            .background(Color(0xFFEEEEEE), RoundedCornerShape(10.dp))
            .scale(scale) // Scale effect when full
            .offset(x = shakeOffsetX.dp) // Shake effect when full
    ) {
        // Progress bar with solid blue color
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                .height(20.dp)
                .background(
                    color = Color(0xFF1A73E8), // Solid blue
                    shape = RoundedCornerShape(10.dp)
                )
        )

        // Water wave overlay
        LottieAnimation(
            composition = waterComposition,
            progress = { waterProgress },
            modifier = Modifier
                .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                .height(20.dp)
                .alpha(0.7f)
        )
    }
}

private fun loadSchedule(prefs: SharedPreferences): List<Pair<String, Float>> {
    val size = prefs.getInt("schedule_size", 0)
    return (0 until size).map { i ->
        val time = prefs.getString("schedule_time_$i", "00:00") ?: "00:00"
        val amount = prefs.getFloat("schedule_amount_$i", 0f)
        time to amount
    }
}

private fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channelId = "water_reminder_channel"
        val channelName = "Water Reminders"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channelId, channelName, importance).apply {
            description = "Reminders to drink water"
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}

@SuppressLint("ScheduleExactAlarm")
private fun scheduleNotifications(context: Context, schedule: List<Pair<String, Float>>, drunkCups: MutableMap<String, Boolean>) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    schedule.forEachIndexed { index, (time, _) ->
        if (!(drunkCups[time] ?: false)) {
            val (hour, minute) = time.split(":").map { it.toInt() }
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                if (before(Calendar.getInstance())) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            // Schedule reminder 5 minutes before
            val fiveMinutesBefore = Calendar.getInstance().apply {
                // Set this calendar to the same time as the original calendar
                timeInMillis = calendar.timeInMillis
                add(Calendar.MINUTE, -5)  // Subtract 5 minutes
                if (before(Calendar.getInstance())) {  // Check if it's before the current time
                    add(Calendar.DAY_OF_YEAR, 1)  // Add 1 day if it's in the past
                }
            }

            val reminderIntent = Intent(context, WaterReminderReceiver::class.java).apply {
                putExtra("notificationId", index + schedule.size) // Unique ID for 5-min reminder
                putExtra("message", "Drink water in 5 minutes!")
            }
            val reminderPendingIntent = PendingIntent.getBroadcast(
                context,
                index + schedule.size,
                reminderIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            if (calendar.timeInMillis > System.currentTimeMillis()) {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    fiveMinutesBefore.timeInMillis,
                    reminderPendingIntent
                )
            }

            // Schedule the main drink notification
            val drinkIntent = Intent(context, WaterReminderReceiver::class.java).apply {
                putExtra("notificationId", index)
                putExtra("message", "Time to Drink Water!")
            }
            val drinkPendingIntent = PendingIntent.getBroadcast(
                context,
                index,
                drinkIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            if (calendar.timeInMillis > System.currentTimeMillis()) {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    drinkPendingIntent
                )
            }
        }
    }
}

private fun cancelNotification(context: Context, time: String, index: Int) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, WaterReminderReceiver::class.java)
    val drinkPendingIntent = PendingIntent.getBroadcast(
        context,
        index,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val reminderPendingIntent = PendingIntent.getBroadcast(
        context,
        index + loadSchedule(context.getSharedPreferences("WaterGoals", Context.MODE_PRIVATE)).size,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    alarmManager.cancel(drinkPendingIntent)
    alarmManager.cancel(reminderPendingIntent)
}

private fun sendCongratulationNotification(context: Context) {
    val notificationManager = NotificationManagerCompat.from(context)
    val channelId = "water_reminder_channel"
    val notificationId = 999 // Unique ID for congratulation

    val builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.dropwiselogo)
        .setContentTitle("Congratulations!")
        .setContentText("You've completed your daily water goal!")
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)

    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
        notificationManager.notify(notificationId, builder.build())
    }
}