package com.example.dropwise

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

data class LoginState(
    val email: String = "",
    val password: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreenUI(
    state: LoginState,
    onValueChange: (LoginState) -> Unit,
    onLoginClick: () -> Unit,
    onGoogleSignInClick: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToResetPassword: () -> Unit, // Added new callback
    snackbarHostState: SnackbarHostState
) {
    var passwordVisible by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }
    var isLogoAnimated by remember { mutableStateOf(false) }

    // Animation values
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
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .graphicsLayer {
                        scaleX = logoScale
                        scaleY = logoScale
                    }
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img),
                    contentDescription = "DropWise Logo",
                    modifier = Modifier
                        .size(80.dp)
                        .align(Alignment.Center)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "DropWise",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4A90E2),
                modifier = Modifier
                    .padding(bottom = 6.dp)
                    .graphicsLayer { alpha = logoScale }
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = formAlpha }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Welcome back!",
                        fontSize = 18.sp,
                        color = Color(0xFF4A90E2),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .padding(vertical = 12.dp)
                            .shadow(elevation = 4.dp, shape = RoundedCornerShape(20.dp)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 20.dp, vertical = 20.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            OutlinedTextField(
                                value = state.email,
                                onValueChange = { onValueChange(state.copy(email = it.trim())) },
                                label = { Text("Email Address", color = Color(0xFF333333)) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Email,
                                        contentDescription = "Email Icon",
                                        tint = Color(0xFF4A90E2),
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFFF5F9FE),
                                    unfocusedContainerColor = Color(0xFFF5F9FE),
                                    focusedIndicatorColor = Color(0xFF4A90E2),
                                    unfocusedIndicatorColor = Color(0xFFDDDDDD)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true,
                                isError = state.email.isBlank() && state.password.isNotBlank(),
                                supportingText = {
                                    AnimatedVisibility(
                                        visible = (state.email.isBlank() && state.password.isNotBlank()) ||
                                                (!state.email.contains("@") && state.email.isNotBlank()),
                                        enter = fadeIn() + expandVertically(),
                                        exit = fadeOut() + shrinkVertically()
                                    ) {
                                        if (state.email.isBlank() && state.password.isNotBlank()) {
                                            Text("Email is required", color = Color.Red)
                                        } else if (!state.email.contains("@") && state.email.isNotBlank()) {
                                            Text("Email must contain @", color = Color.Red)
                                        }
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = state.password,
                                onValueChange = { onValueChange(state.copy(password = it)) },
                                label = { Text("Password", color = Color(0xFF333333)) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = "Password Icon",
                                        tint = Color(0xFF4A90E2),
                                        modifier = Modifier.size(20.dp)
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
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true,
                                isError = state.password.isBlank() && state.email.isNotBlank(),
                                supportingText = {
                                    AnimatedVisibility(
                                        visible = state.password.isBlank() && state.email.isNotBlank(),
                                        enter = fadeIn() + expandVertically(),
                                        exit = fadeOut() + shrinkVertically()
                                    ) {
                                        Text("Password is required", color = Color.Red)
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            TextButton(
                                onClick = onNavigateToResetPassword, // Updated to navigate to reset password screen
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(
                                    text = "Reset Password?", // Changed from "Forgot Password?"
                                    color = Color(0xFF4A90E2),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = onLoginClick,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4A90E2),
                                    disabledContainerColor = Color(0xFF4A90E2).copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .graphicsLayer {
                                        scaleX = buttonScale
                                        scaleY = buttonScale
                                    },
                                shape = RoundedCornerShape(10.dp),
                                enabled = state.email.isNotBlank() && state.password.isNotBlank()
                            ) {
                                Text(
                                    "LOG IN",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Divider(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 12.dp),
                                    color = Color(0xFFDDDDDD)
                                )
                                Text(
                                    text = "OR",
                                    color = Color(0xFF666666),
                                    fontSize = 12.sp
                                )
                                Divider(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 12.dp),
                                    color = Color(0xFFDDDDDD)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = onGoogleSignInClick,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color(0xFF757575)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .graphicsLayer {
                                        scaleX = buttonScale
                                        scaleY = buttonScale
                                    }
                                    .border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(10.dp)),
                                shape = RoundedCornerShape(10.dp)
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
                                        "Sign in with Google",
                                        color = Color(0xFF757575),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Text(
                            text = "Don't have an account? ",
                            color = Color(0xFF666666),
                            fontSize = 12.sp
                        )
                        TextButton(onClick = onNavigateToRegister) {
                            Text(
                                text = "Sign Up",
                                color = Color(0xFF4A90E2),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(80.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 12.dp)
        ) { data ->
            Snackbar(
                modifier = Modifier.padding(horizontal = 12.dp),
                shape = RoundedCornerShape(10.dp),
                containerColor = Color(0xFF323232),
                contentColor = Color.White
            ) {
                Text(text = data.visuals.message)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPasswordScreen(
    onResetPassword: (String) -> Unit,
    onBackToLogin: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    var email by remember { mutableStateOf("") }
    var isVisible by remember { mutableStateOf(false) }

    val formAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 800)
    )

    LaunchedEffect(Unit) {
        delay(200)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .padding(vertical = 12.dp)
                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(20.dp)),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .graphicsLayer { alpha = formAlpha },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Reset Password",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4A90E2),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Text(
                        text = "Enter your email to receive a password reset link",
                        fontSize = 14.sp,
                        color = Color(0xFF666666),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it.trim() },
                        label = { Text("Email Address", color = Color(0xFF333333)) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = "Email Icon",
                                tint = Color(0xFF4A90E2),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF5F9FE),
                            unfocusedContainerColor = Color(0xFFF5F9FE),
                            focusedIndicatorColor = Color(0xFF4A90E2),
                            unfocusedIndicatorColor = Color(0xFFDDDDDD)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        isError = email.isNotBlank() && !email.contains("@"),
                        supportingText = {
                            if (email.isNotBlank() && !email.contains("@")) {
                                Text("Please enter a valid email", color = Color.Red)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { onResetPassword(email) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90E2)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        enabled = email.isNotBlank() && email.contains("@")
                    ) {
                        Text(
                            "Send Reset Link",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(onClick = onBackToLogin) {
                        Text(
                            text = "Back to Login",
                            color = Color(0xFF4A90E2),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(12.dp)
        ) { data ->
            Snackbar(
                modifier = Modifier.padding(12.dp),
                shape = RoundedCornerShape(10.dp),
                containerColor = Color(0xFF323232),
                contentColor = Color.White
            ) {
                Text(text = data.visuals.message)
            }
        }
    }
}