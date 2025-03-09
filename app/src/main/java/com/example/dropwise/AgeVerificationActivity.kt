package com.example.dropwise

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class AgeVerificationActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth // Add FirebaseAuth instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance() // Initialize FirebaseAuth
        setContent {
            AgeVerificationScreen()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun AgeVerificationScreen() {
        val scope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }
        val db = Room.databaseBuilder(this@AgeVerificationActivity, AppDatabase::class.java, "dropwise_db")
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()

        // Get Google account details from intent
        val email = intent.getStringExtra("email") ?: ""
        val username = intent.getStringExtra("username") ?: ""
        val googleId = intent.getStringExtra("googleId") ?: ""

        // Get Firebase UID
        val userId = auth.currentUser?.uid ?: run {
            snackbarHostState.currentSnackbarData?.dismiss()
            scope.launch {
                snackbarHostState.showSnackbar("User not authenticated. Please sign in again.")
                startActivity(Intent(this@AgeVerificationActivity, RegisterActivity::class.java))
                finish()
            }
            return@AgeVerificationScreen
        }

        // Calculate the maximum year (current year - 18)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val maxYear = currentYear - 18

        // State for date selection
        var day by remember { mutableStateOf("") }
        var month by remember { mutableStateOf("") }
        var year by remember { mutableStateOf("") }
        var showDatePickerDialog by remember { mutableStateOf(false) }

        LaunchedEffect(showDatePickerDialog) {
            println("showDatePickerDialog changed to: $showDatePickerDialog")
        }

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(
                        top = paddingValues.calculateTopPadding() + 32.dp,
                        bottom = paddingValues.calculateBottomPadding() + 32.dp,
                        start = 32.dp,
                        end = 32.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Verify Your Age",
                    fontSize = 32.sp,
                    color = Color(0xFF333333),
                    style = MaterialTheme.typography.headlineLarge
                )
                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                showDatePickerDialog = true
                                println("Date picker clicked: showDatePickerDialog = $showDatePickerDialog")
                            }
                        )
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Date Picker Icon",
                        tint = Color(0xFF4A90E2),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (day.isNotEmpty() && month.isNotEmpty() && year.isNotEmpty())
                            "$day $month $year"
                        else
                            "Select Date of Birth",
                        fontSize = 16.sp,
                        color = if (day.isEmpty()) Color(0xFF333333) else Color.Black,
                        fontWeight = if (day.isEmpty()) FontWeight.Normal else FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        scope.launch {
                            try {
                                if (day.isBlank() || month.isBlank() || year.isBlank()) {
                                    snackbarHostState.showSnackbar("Please select your date of birth")
                                    return@launch
                                }
                                val birthday = "$year-${monthToNumber(month)}-${day.padStart(2, '0')}"
                                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                sdf.isLenient = false
                                val birthDate = withContext(Dispatchers.IO) { sdf.parse(birthday) }
                                    ?: throw IllegalArgumentException("Invalid date format")
                                val age = currentYear - (birthDate.year + 1900) // Correct year calculation
                                if (age >= 18) {
                                    // Create user with Firebase UID and save to Room
                                    val newUser = User(
                                        id = userId, // Use Firebase UID
                                        username = username,
                                        email = email,
                                        password = "", // No password for Google auth
                                        birthday = birthday
                                    )
                                    withContext(Dispatchers.IO) { db.userDao().insert(newUser) }

                                    // Login and redirect
                                    SessionManager.login(this@AgeVerificationActivity, userId)
                                    snackbarHostState.showSnackbar("Age verified! Redirecting to Dashboard.")
                                    startActivity(Intent(this@AgeVerificationActivity, MainActivity::class.java))
                                    finish()
                                } else {
                                    snackbarHostState.showSnackbar("You must be 18 or older to access the dashboard.")
                                }
                            } catch (e: Exception) {
                                Log.e("AgeVerification", "Age verification failed: ${e.message}", e)
                                snackbarHostState.showSnackbar("Error: ${e.message ?: "Unknown error"}")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90E2)),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(text = "Verify Age", color = Color.White, fontSize = 18.sp)
                }
            }

            if (showDatePickerDialog) {
                println("Rendering DatePickerDialog")
                DatePickerDialog(
                    initialDay = day.toIntOrNull() ?: 1,
                    initialMonth = when (month.lowercase()) {
                        "jan" -> 1
                        "feb" -> 2
                        "mar" -> 3
                        "apr" -> 4
                        "may" -> 5
                        "jun" -> 6
                        "jul" -> 7
                        "aug" -> 8
                        "sep" -> 9
                        "oct" -> 10
                        "nov" -> 11
                        "dec" -> 12
                        else -> 1
                    },
                    initialYear = year.toIntOrNull() ?: maxYear,
                    maxYear = maxYear,
                    onConfirm = { selectedDay, selectedMonth, selectedYear ->
                        day = selectedDay.toString()
                        month = selectedMonth
                        year = selectedYear.toString()
                        showDatePickerDialog = false
                        println("Date confirmed: $day $month $year")
                    },
                    onDismiss = {
                        showDatePickerDialog = false
                        println("DatePickerDialog dismissed")
                    }
                )
            }
        }
    }

    @Composable
    fun DatePickerDialog(
        initialDay: Int,
        initialMonth: Int,
        initialYear: Int,
        maxYear: Int,
        onConfirm: (Int, String, Int) -> Unit,
        onDismiss: () -> Unit
    ) {
        var selectedDay by remember { mutableStateOf(initialDay.coerceIn(1, 31)) }
        var selectedMonth by remember { mutableStateOf(initialMonth.coerceIn(1, 12)) }
        var selectedYear by remember { mutableStateOf(initialYear.coerceIn(1980, maxYear)) }

        val dayState = rememberLazyListState()
        val monthState = rememberLazyListState()
        val yearState = rememberLazyListState()

        LaunchedEffect(Unit) {
            dayState.scrollToItem((selectedDay - 1).coerceAtLeast(0))
            monthState.scrollToItem((selectedMonth - 1).coerceAtLeast(0))
            yearState.scrollToItem((maxYear - selectedYear).coerceAtLeast(0))
            println("DatePickerDialog Launched: Day=$selectedDay, Month=$selectedMonth, Year=$selectedYear")
        }

        Dialog(onDismissRequest = onDismiss) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                modifier = Modifier
                    .width(340.dp)
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFF4A90E2).copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "Select Your Birth Date",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF4A90E2),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF5F6F5), RoundedCornerShape(8.dp))
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        LazyColumn(
                            state = dayState,
                            modifier = Modifier
                                .weight(1f)
                                .height(120.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            items(31) { index ->
                                val day = index + 1
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedDay = day }
                                        .background(
                                            if (day == selectedDay) Color(0xFF4A90E2).copy(alpha = 0.1f) else Color.Transparent,
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(vertical = 8.dp)
                                ) {
                                    Text(
                                        text = day.toString(),
                                        color = if (day == selectedDay) Color(0xFF4A90E2) else Color(0xFF333333),
                                        fontSize = 16.sp,
                                        fontWeight = if (day == selectedDay) FontWeight.Bold else FontWeight.Normal,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        LazyColumn(
                            state = monthState,
                            modifier = Modifier
                                .weight(1.5f)
                                .height(120.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                            items(months) { month ->
                                val monthIndex = months.indexOf(month) + 1
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedMonth = monthIndex }
                                        .background(
                                            if (monthIndex == selectedMonth) Color(0xFF4A90E2).copy(alpha = 0.1f) else Color.Transparent,
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(vertical = 8.dp)
                                ) {
                                    Text(
                                        text = month,
                                        color = if (monthIndex == selectedMonth) Color(0xFF4A90E2) else Color(0xFF333333),
                                        fontSize = 16.sp,
                                        fontWeight = if (monthIndex == selectedMonth) FontWeight.Bold else FontWeight.Normal,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        LazyColumn(
                            state = yearState,
                            modifier = Modifier
                                .weight(1.2f)
                                .height(120.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val years = (maxYear downTo 1980).toList()
                            items(years) { year ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedYear = year }
                                        .background(
                                            if (year == selectedYear) Color(0xFF4A90E2).copy(alpha = 0.1f) else Color.Transparent,
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(vertical = 8.dp)
                                ) {
                                    Text(
                                        text = year.toString(),
                                        color = if (year == selectedYear) Color(0xFF4A90E2) else Color(0xFF333333),
                                        fontSize = 16.sp,
                                        fontWeight = if (year == selectedYear) FontWeight.Bold else FontWeight.Normal,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(
                                text = "Cancel",
                                color = Color(0xFF333333),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Button(
                            onClick = {
                                val selectedMonthAbbreviation = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")[selectedMonth - 1]
                                onConfirm(selectedDay, selectedMonthAbbreviation, selectedYear)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90E2)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "Confirm",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }

    private fun monthToNumber(month: String): String {
        return when (month.lowercase()) {
            "jan" -> "01"
            "feb" -> "02"
            "mar" -> "03"
            "apr" -> "04"
            "may" -> "05"
            "jun" -> "06"
            "jul" -> "07"
            "aug" -> "08"
            "sep" -> "09"
            "oct" -> "10"
            "nov" -> "11"
            "dec" -> "12"
            else -> "01" // Default to January if invalid
        }
    }
}