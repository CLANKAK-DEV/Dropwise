package com.example.dropwise

import android.annotation.SuppressLint
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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog

data class RegistrationState(
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val day: String = "",
    val month: String = "",
    val year: String = ""
)

// Simple email validation function
fun isEmailValid(email: String): Boolean {
    val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()
    return email.matches(emailRegex)
}

@SuppressLint("UnrememberedMutableInteractionSource")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreenUI(
    state: RegistrationState,
    onValueChange: (RegistrationState) -> Unit,
    onRegisterClick: () -> Unit,
    onGoogleSignInClick: () -> Unit,
    onNavigateToLogin: () -> Unit,
    maxYear: Int,
    snackbarHostState: SnackbarHostState
) {
    var passwordVisible by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var hasSubmitted by remember { mutableStateOf(false) }

    LaunchedEffect(showDatePickerDialog) {
        println("showDatePickerDialog changed to: $showDatePickerDialog")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Create Account",
                fontSize = 20.sp,
                color = Color(0xFF1E88E5),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = state.username,
                onValueChange = { newValue ->
                    if (newValue.all { char -> char.isLetter() || char.isWhitespace() }) {
                        onValueChange(state.copy(username = newValue.trim()))
                    }
                },
                label = {
                    Text(
                        text = if (hasSubmitted && state.username.isBlank()) "Username is required"
                        else "Username",
                        color = if (hasSubmitted && state.username.isBlank()) Color(0xFFD32F2F) else Color(0xFF455A64)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Username Icon",
                        tint = Color(0xFF1E88E5)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF1E88E5),
                    unfocusedBorderColor = Color(0xFF455A64),
                    cursorColor = Color(0xFF1E88E5),
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                ),
                textStyle = TextStyle(color = Color.Black, fontSize = 16.sp),
                singleLine = true,
                shape = RoundedCornerShape(0.dp),
                isError = hasSubmitted && state.username.isBlank()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = state.email,
                onValueChange = { onValueChange(state.copy(email = it.trim())) },
                label = {
                    Text(
                        text = when {
                            hasSubmitted && state.email.isBlank() -> "Email is required"
                            hasSubmitted && !isEmailValid(state.email) -> "Invalid email format"
                            else -> "Email Address"
                        },
                        color = if (hasSubmitted && (state.email.isBlank() || !isEmailValid(state.email))) Color(0xFFD32F2F)
                        else Color(0xFF455A64)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Email Icon",
                        tint = Color(0xFF1E88E5)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF1E88E5),
                    unfocusedBorderColor = Color(0xFF455A64),
                    cursorColor = Color(0xFF1E88E5),
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                ),
                textStyle = TextStyle(color = Color.Black, fontSize = 16.sp),
                singleLine = true,
                shape = RoundedCornerShape(0.dp),
                isError = hasSubmitted && (state.email.isBlank() || !isEmailValid(state.email))
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = state.password,
                onValueChange = { onValueChange(state.copy(password = it)) },
                label = {
                    Text(
                        text = if (hasSubmitted && state.password.isBlank()) "Password is required"
                        else "Password",
                        color = if (hasSubmitted && state.password.isBlank()) Color(0xFFD32F2F) else Color(0xFF455A64)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Password Icon",
                        tint = Color(0xFF1E88E5)
                    )
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = Color(0xFF455A64)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF1E88E5),
                    unfocusedBorderColor = Color(0xFF455A64),
                    cursorColor = Color(0xFF1E88E5),
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                ),
                textStyle = TextStyle(color = Color.Black, fontSize = 16.sp),
                singleLine = true,
                shape = RoundedCornerShape(0.dp),
                isError = hasSubmitted && state.password.isBlank()
            )

            Spacer(modifier = Modifier.height(12.dp))

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
                    tint = Color(0xFF1E88E5),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (state.day.isNotEmpty() && state.month.isNotEmpty() && state.year.isNotEmpty())
                        "${state.day} ${state.month} ${state.year}"
                    else if (hasSubmitted && (state.day.isBlank() || state.month.isBlank() || state.year.isBlank()))
                        "Date is required"
                    else
                        "Select Date of Birth",
                    fontSize = 16.sp,
                    color = if (hasSubmitted && (state.day.isBlank() || state.month.isBlank() || state.year.isBlank())) Color(0xFFD32F2F)
                    else if (state.day.isEmpty()) Color(0xFF455A64) else Color.Black,
                    fontWeight = if (state.day.isEmpty()) FontWeight.Normal else FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    hasSubmitted = true
                    onRegisterClick()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color(0xFFF06292)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(Color.Transparent)
                    .border(1.dp, Color(0xFFF06292), RoundedCornerShape(0.dp)),
                shape = RoundedCornerShape(0.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                enabled = state.username.isNotBlank() && state.email.isNotBlank() && isEmailValid(state.email) &&
                        state.password.isNotBlank() && state.day.isNotBlank() && state.month.isNotBlank() && state.year.isNotBlank()
            ) {
                Text(
                    "SIGN UP",
                    color = Color(0xFFF06292),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onGoogleSignInClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color(0xFF4285F4)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(Color.Transparent)
                    .border(1.dp, Color(0xFF4285F4), RoundedCornerShape(0.dp)),
                shape = RoundedCornerShape(0.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    "Sign up with Google",
                    color = Color(0xFF4285F4),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(onClick = onNavigateToLogin) {
                Text(
                    text = "Already have an account? Sign in",
                    color = Color(0xFF1E88E5),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        if (showDatePickerDialog) {
            println("Rendering DatePickerDialog")
            DatePickerDialog(
                initialDay = state.day.toIntOrNull() ?: 1,
                initialMonth = when (state.month.lowercase()) {
                    "janv." -> 1
                    "févr." -> 2
                    "mars" -> 3
                    "avr." -> 4
                    "mai" -> 5
                    "juin" -> 6
                    "juil." -> 7
                    "août" -> 8
                    "sept." -> 9
                    "oct." -> 10
                    "nov." -> 11
                    "déc." -> 12
                    else -> 1
                },
                initialYear = state.year.toIntOrNull() ?: maxYear,
                maxYear = maxYear,
                onConfirm = { day, month, year ->
                    onValueChange(state.copy(day = day.toString(), month = month, year = year.toString()))
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
                .border(1.dp, Color(0xFF1E88E5).copy(alpha = 0.2f), RoundedCornerShape(16.dp))
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
                    color = Color(0xFF1E88E5),
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
                                        if (day == selectedDay) Color(0xFF1E88E5).copy(alpha = 0.1f) else Color.Transparent,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = day.toString(),
                                    color = if (day == selectedDay) Color(0xFF1E88E5) else Color(0xFF455A64),
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
                        val months = listOf(
                            "janv.", "févr.", "mars", "avr.", "mai", "juin",
                            "juil.", "août", "sept.", "oct.", "nov.", "déc."
                        )
                        items(months) { month ->
                            val monthIndex = months.indexOf(month) + 1
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedMonth = monthIndex }
                                    .background(
                                        if (monthIndex == selectedMonth) Color(0xFF1E88E5).copy(alpha = 0.1f) else Color.Transparent,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = month,
                                    color = if (monthIndex == selectedMonth) Color(0xFF1E88E5) else Color(0xFF455A64),
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
                                        if (year == selectedYear) Color(0xFF1E88E5).copy(alpha = 0.1f) else Color.Transparent,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = year.toString(),
                                    color = if (year == selectedYear) Color(0xFF1E88E5) else Color(0xFF455A64),
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
                            color = Color(0xFF455A64),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Button(
                        onClick = {
                            val selectedMonthAbbreviation = listOf(
                                "janv.", "févr.", "mars", "avr.", "mai", "juin",
                                "juil.", "août", "sept.", "oct.", "nov.", "déc."
                            )[selectedMonth - 1]
                            onConfirm(selectedDay, selectedMonthAbbreviation, selectedYear)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
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

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun RegisterScreenPreview() {
    val snackbarHostState = remember { SnackbarHostState() }
    RegisterScreenUI(
        state = RegistrationState(),
        onValueChange = {},
        onRegisterClick = {},
        onGoogleSignInClick = {},
        onNavigateToLogin = {},
        maxYear = 2025,
        snackbarHostState = snackbarHostState
    )
}