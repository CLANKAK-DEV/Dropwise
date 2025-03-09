package com.example.dropwise

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    snackbarHostState: SnackbarHostState
) {
    var passwordVisible by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE6F0FA))
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(durationMillis = 1000)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Welcome back!",
                    fontSize = 24.sp,
                    color = Color(0xFF4A90E2),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(vertical = 16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        OutlinedTextField(
                            value = state.email,
                            onValueChange = { onValueChange(state.copy(email = it.trim())) },
                            label = { Text("Email Address", color = Color(0xFF333333)) },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email Icon", tint = Color(0xFF4A90E2)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color(0xFF4A90E2),
                                unfocusedIndicatorColor = Color(0xFF333333)
                            ),
                            singleLine = true,
                            isError = state.email.isBlank() && state.password.isNotBlank(),
                            supportingText = {
                                if (state.email.isBlank() && state.password.isNotBlank()) {
                                    Text("Email is required", color = Color.Red)
                                } else if (!state.email.contains("@") && state.email.isNotBlank()) {
                                    Text("Email must contain @", color = Color.Red)
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = state.password,
                            onValueChange = { onValueChange(state.copy(password = it)) },
                            label = { Text("Password", color = Color(0xFF333333)) },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password Icon", tint = Color(0xFF4A90E2)) },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color(0xFF4A90E2),
                                unfocusedIndicatorColor = Color(0xFF333333)
                            ),
                            singleLine = true,
                            isError = state.password.isBlank() && state.email.isNotBlank(),
                            supportingText = {
                                if (state.password.isBlank() && state.email.isNotBlank()) {
                                    Text("Password is required", color = Color.Red)
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = { /* Forgot Password logic */ },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(
                                text = "Forgot Password?",
                                color = Color(0xFF4A90E2),
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = onLoginClick,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90E2)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(8.dp),
                            enabled = state.email.isNotBlank() && state.password.isNotBlank()
                        ) {
                            Text("LOGIN", color = Color.White, fontSize = 18.sp)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = onGoogleSignInClick,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Sign in with Google", color = Color.White, fontSize = 18.sp)
                        }
                    }
                }

                TextButton(
                    onClick = onNavigateToRegister,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text(
                        text = "Don’t have an account? Sign up",
                        color = Color(0xFF4A90E2),
                        fontSize = 16.sp,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}