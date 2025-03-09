package com.example.dropwise

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UpdateEmailActivity : ComponentActivity() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UpdateEmailScreen(
                onBackClick = { finish() } // Navigate back to AccountScreen
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun UpdateEmailScreen(onBackClick: () -> Unit) {
        val user = auth.currentUser
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        val snackbarHostState = remember { SnackbarHostState() }

        var currentEmail by remember { mutableStateOf(user?.email ?: "N/A") }
        var newEmail by remember { mutableStateOf(TextFieldValue("")) }
        var isLoading by remember { mutableStateOf(false) }
        var hasSubmitted by remember { mutableStateOf(false) }

        // Simple email validation
        fun isEmailValid(email: String): Boolean {
            val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()
            return email.matches(emailRegex) && email.isNotBlank()
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text("Mettre à jour l'email") },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF4A90E2)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Email actuel: $currentEmail",
                    fontSize = 16.sp,
                    color = Color(0xFF333333)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = newEmail,
                    onValueChange = { newEmail = it },
                    label = {
                        Text(
                            text = when {
                                hasSubmitted && newEmail.text.isBlank() -> "Email requis"
                                hasSubmitted && !isEmailValid(newEmail.text) -> "Email invalide"
                                else -> "Nouvel email"
                            },
                            color = if (hasSubmitted && (!isEmailValid(newEmail.text) || newEmail.text.isBlank())) Color(0xFFD32F2F)
                            else Color(0xFF455A64)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4A90E2),
                        unfocusedBorderColor = Color(0xFF455A64),
                        cursorColor = Color(0xFF4A90E2),
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    ),
                    singleLine = true,
                    isError = hasSubmitted && (!isEmailValid(newEmail.text) || newEmail.text.isBlank())
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        hasSubmitted = true
                        if (isEmailValid(newEmail.text)) {
                            isLoading = true
                            scope.launch {
                                try {
                                    user?.let {
                                        // Update email in Firebase Auth
                                        it.updateEmail(newEmail.text).await()

                                        // Update email in Firestore (optional)
                                        firestore.collection("users").document(it.uid)
                                            .update("email", newEmail.text)
                                            .await()

                                        currentEmail = newEmail.text
                                        newEmail = TextFieldValue("") // Clear input
                                        snackbarHostState.showSnackbar("Email mis à jour avec succès")
                                    }
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Erreur: ${e.message}")
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90E2)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(text = "Mettre à jour", color = Color.White, fontSize = 18.sp)
                    }
                }
            }
        }
    }
}