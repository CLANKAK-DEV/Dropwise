package com.example.dropwise

import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay

data class RegistrationState(
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val day: String = "",
    val month: String = "",
    val year: String = ""
)

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
    modifier: Modifier = Modifier // Add this line
,
            snackbarHostState: SnackbarHostState
) {
    var passwordVisible by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var hasSubmitted by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }
    var isLogoAnimated by remember { mutableStateOf(false) }

    val logoScale by animateFloatAsState(
        targetValue = if (isLogoAnimated) 1f else 0.5f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
    )

    val formAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 800, delayMillis = 300)
    )

    val buttonScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.8f,
        animationSpec = tween(durationMillis = 500, delayMillis = 800)
    )

    LaunchedEffect(Unit) {
        isLogoAnimated = true
        delay(500)
        isVisible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE6F0FA),
                        Color(0xFFCCE4F7)
                    )
                )
            )
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF4A90E2).copy(alpha = 0.3f),
                            Color(0xFFE6F0FA).copy(alpha = 0f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(40.dp)) // Reduced from 60.dp to 40.dp

            Box(
                modifier = Modifier
                    .size(120.dp) // Increased Box size from 80.dp to 120.dp
                    .graphicsLayer {
                        scaleX = logoScale
                        scaleY = logoScale
                    }
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img),
                    contentDescription = "DropWise Logo",
                    modifier = Modifier
                        .size(80.dp) // Increased Image size from 40.dp to 80.dp
                        .align(Alignment.Center)
                )
            }


            Spacer(modifier = Modifier.height(12.dp)) // Reduced from 16.dp to 12.dp

            Text(
                text = "DropWise",
                fontSize = 24.sp, // Reduced from 32.sp to 24.sp
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4A90E2),
                modifier = Modifier
                    .padding(bottom = 6.dp) // Reduced from 8.dp to 6.dp
                    .graphicsLayer {
                        alpha = logoScale
                    }
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = formAlpha
                    }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Create Account",
                        fontSize = 18.sp, // Reduced from 24.sp to 18.sp
                        color = Color(0xFF4A90E2),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 12.dp) // Reduced from 16.dp to 12.dp
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .padding(vertical = 16.dp)
                            .shadow(elevation = 6.dp, shape = RoundedCornerShape(24.dp)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 24.dp, vertical = 24.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            OutlinedTextField(
                                value = state.username,
                                onValueChange = {
                                    if (it.all { char -> char.isLetter() || char.isWhitespace() }) {
                                        onValueChange(state.copy(username = it.trim()))
                                    }
                                },
                                label = {
                                    Text(
                                        text = if (hasSubmitted && state.username.isBlank()) "Username is required"
                                        else "Username",
                                        color = if (hasSubmitted && state.username.isBlank()) Color.Red else Color(0xFF333333)
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = "Username Icon",
                                        tint = Color(0xFF4A90E2),
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFFF5F9FE),
                                    unfocusedContainerColor = Color(0xFFF5F9FE),
                                    focusedIndicatorColor = Color(0xFF4A90E2),
                                    unfocusedIndicatorColor = Color(0xFFDDDDDD)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                isError = hasSubmitted && state.username.isBlank()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

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
                                        color = if (hasSubmitted && (state.email.isBlank() || !isEmailValid(state.email))) Color.Red else Color(0xFF333333)
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Email,
                                        contentDescription = "Email Icon",
                                        tint = Color(0xFF4A90E2),
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFFF5F9FE),
                                    unfocusedContainerColor = Color(0xFFF5F9FE),
                                    focusedIndicatorColor = Color(0xFF4A90E2),
                                    unfocusedIndicatorColor = Color(0xFFDDDDDD)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                isError = hasSubmitted && (state.email.isBlank() || !isEmailValid(state.email))
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = state.password,
                                onValueChange = { onValueChange(state.copy(password = it)) },
                                label = {
                                    Text(
                                        text = if (hasSubmitted && state.password.isBlank()) "Password is required"
                                        else "Password",
                                        color = if (hasSubmitted && state.password.isBlank()) Color.Red else Color(0xFF333333)
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = "Password Icon",
                                        tint = Color(0xFF4A90E2),
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                            tint = Color(0xFF4A90E2)
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFFF5F9FE),
                                    unfocusedContainerColor = Color(0xFFF5F9FE),
                                    focusedIndicatorColor = Color(0xFF4A90E2),
                                    unfocusedIndicatorColor = Color(0xFFDDDDDD)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                isError = hasSubmitted && state.password.isBlank()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { showDatePickerDialog = true }
                                    )
                                    .padding(vertical = 12.dp)
                                    .border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF5F9FE), RoundedCornerShape(12.dp)),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = "Date Picker Icon",
                                    tint = Color(0xFF4A90E2),
                                    modifier = Modifier
                                        .padding(start = 12.dp)
                                        .size(24.dp)
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
                                    color = if (hasSubmitted && (state.day.isBlank() || state.month.isBlank() || state.year.isBlank())) Color.Red
                                    else if (state.day.isEmpty()) Color(0xFF333333) else Color.Black,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    hasSubmitted = true
                                    onRegisterClick()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4A90E2),
                                    disabledContainerColor = Color(0xFF4A90E2).copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .graphicsLayer {
                                        scaleX = buttonScale
                                        scaleY = buttonScale
                                    },
                                shape = RoundedCornerShape(12.dp),
                                enabled = state.username.isNotBlank() && state.email.isNotBlank() && isEmailValid(state.email) &&
                                        state.password.isNotBlank() && state.day.isNotBlank() && state.month.isNotBlank() && state.year.isNotBlank()
                            ) {
                                Text(
                                    "SIGN UP",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Divider(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 16.dp),
                                    color = Color(0xFFDDDDDD)
                                )
                                Text(
                                    text = "OR",
                                    color = Color(0xFF666666),
                                    fontSize = 14.sp
                                )
                                Divider(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 16.dp),
                                    color = Color(0xFFDDDDDD)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = onGoogleSignInClick,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color(0xFF757575)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .graphicsLayer {
                                        scaleX = buttonScale
                                        scaleY = buttonScale
                                    }
                                    .border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(12.dp)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.cejd),
                                        contentDescription = "Google Logo",
                                        modifier = Modifier
                                            .size(24.dp)
                                            .padding(end = 8.dp)
                                    )
                                    Text(
                                        "Sign up with Google",
                                        color = Color(0xFF757575),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically, // Ensures both text and button are aligned
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Text(
                                    text = "Already have an account? ",
                                    color = Color(0xFF666666),
                                    fontSize = 14.sp
                                )
                                TextButton(onClick = onNavigateToLogin) {
                                    Text(
                                        text = "Sign In",
                                        color = Color(0xFF4A90E2),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(100.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        ) { data ->
            Snackbar(
                modifier = Modifier.padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                containerColor = Color(0xFF323232),
                contentColor = Color.White
            ) {
                Text(text = data.visuals.message)
            }
        }

        if (showDatePickerDialog) {
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
                },
                onDismiss = { showDatePickerDialog = false }
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
                        .background(Color(0xFFF5F9FE), RoundedCornerShape(8.dp))
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
                            color = Color(0xFF666666),
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
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90E2)),
                        shape = RoundedCornerShape(12.dp)
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

