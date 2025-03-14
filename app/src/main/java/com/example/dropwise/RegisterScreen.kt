package com.example.dropwise

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*



class RegisterActivity : ComponentActivity() {
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private val currentYear: Int = Calendar.getInstance().get(Calendar.YEAR)
    private val maxYear: Int = currentYear - 18
    private val TAG = "RegisterActivity"

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                showErrorMessage("Google sign-up failed: ${e.statusCode}")
                Log.e(TAG, "Google sign-in failed", e)
            }
        } else {
            showErrorMessage("Google sign-up cancelled")
        }
    }

    private fun showErrorMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .requestIdToken(getString(R.string.default_web_client_id))
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setContent {
            val scope = rememberCoroutineScope()
            var state by remember { mutableStateOf(RegistrationState()) }
            val snackbarHostState = remember { SnackbarHostState() }

            RegisterScreenUI(
                state = state,
                onValueChange = { newState -> state = newState },
                onRegisterClick = {
                    registerWithEmailPassword(state, scope, snackbarHostState)
                },
                onGoogleSignInClick = {
                    signInWithGoogle()
                },
                onNavigateToLogin = {
                    startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                    finish()
                },
                maxYear = maxYear,
                snackbarHostState = snackbarHostState,
                modifier = Modifier // Error here
            )
        }
    }

    private fun registerWithEmailPassword(state: RegistrationState, scope: CoroutineScope, snackbarHostState: SnackbarHostState) {
        scope.launch {
            try {
                Log.d(TAG, "Starting registration with state: $state")
                // Step 1: Validation
                if (state.username.isBlank()) throw Exception("Username is required")
                if (state.email.isBlank() || !isEmailValid(state.email)) throw Exception("Invalid email")
                if (state.password.isBlank()) throw Exception("Password is required")
                if (state.password.length < 6) throw Exception("Password must be at least 6 characters")
                if (state.day.isBlank() || state.month.isBlank() || state.year.isBlank()) throw Exception("Date of birth is required")

                val birthday = "${state.year}-${monthToNumber(state.month)}-${state.day.padStart(2, '0')}"
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                sdf.isLenient = false
                val birthDate = sdf.parse(birthday) ?: throw Exception("Invalid date of birth")
                val age = currentYear - SimpleDateFormat("yyyy", Locale.getDefault()).format(birthDate).toInt()
                if (age < 18) throw Exception("You must be at least 18 years old")

                Log.d(TAG, "Validation passed, registering user with email: ${state.email}")
                // Step 2: Register with Firebase Auth
                val authResult = withContext(Dispatchers.IO) {
                    auth.createUserWithEmailAndPassword(state.email, state.password).await()
                }
                val userId = authResult.user?.uid ?: throw Exception("User ID not found")
                val roomId = "room_$userId"

                Log.d(TAG, "Firebase Auth successful, userId: $userId")
                // Step 3: Save to Firestore
                val userMap = hashMapOf(
                    "username" to state.username,
                    "email" to state.email,
                    "birthday" to birthday,
                    "roomId" to roomId,
                    "createdAt" to System.currentTimeMillis()
                )
                val roomMap = hashMapOf(
                    "userId" to userId,
                    "createdAt" to System.currentTimeMillis()
                )
                withContext(Dispatchers.IO) {
                    firestore.collection("users").document(userId).set(userMap).await()
                    firestore.collection("rooms").document(roomId).set(roomMap).await()
                }
                Log.d(TAG, "Firestore save successful")

                // Step 4: Save to Room (optional, proceed even if it fails)
                try {
                    val db = AppDatabase.getDatabase(this@RegisterActivity)
                    val user = User(
                        id = userId,
                        username = state.username,
                        email = state.email,
                        password = state.password,
                        birthday = birthday,
                        roomId = roomId
                    )
                    withContext(Dispatchers.IO) {
                        db.userDao().insert(user)
                    }
                    Log.d(TAG, "Room save successful")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to save to Room: ${e.message}", e)
                    snackbarHostState.showSnackbar("Local storage failed, but account created.")
                }

                // Step 5: Login and Navigate
                SessionManager.login(this@RegisterActivity, userId)
                snackbarHostState.showSnackbar("Registration successful! Redirecting to Dashboard.")
                startActivity(Intent(this@RegisterActivity, MainActivity::class.java))
                finish()
                Log.d(TAG, "Navigation to MainActivity successful")

            } catch (e: FirebaseAuthException) {
                val errorMessage = when (e.errorCode) {
                    "ERROR_EMAIL_ALREADY_IN_USE" -> "This email is already in use. Please use a different email."
                    "ERROR_WEAK_PASSWORD" -> "Password is too weak. It must be at least 6 characters."
                    "ERROR_INVALID_EMAIL" -> "Invalid email format."
                    else -> "Registration failed: ${e.message ?: "Unknown error"}"
                }
                Log.e(TAG, "Firebase Auth error: ${e.errorCode} - ${e.message}", e)
                snackbarHostState.showSnackbar(errorMessage)
            } catch (e: Exception) {
                Log.e(TAG, "Registration failed: ${e.message}", e)
                snackbarHostState.showSnackbar("Registration failed: ${e.message ?: "Unknown error"}")
            }
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val authResult = withContext(Dispatchers.IO) {
                    auth.signInWithCredential(credential).await()
                }
                val userId = authResult.user?.uid ?: throw Exception("User ID not found")
                val email = account.email ?: ""
                val username = account.displayName ?: "Google User"

                // Redirect to AgeVerificationActivity instead of completing registration here
                val intent = Intent(this@RegisterActivity, AgeVerificationActivity::class.java).apply {
                    putExtra("email", email)
                    putExtra("username", username)
                    putExtra("googleId", userId) // Pass Firebase UID
                }
                startActivity(intent)
                finish() // Finish RegisterActivity to prevent going back

            } catch (e: Exception) {
                Log.e(TAG, "Google sign-up failed: ${e.message}", e)
                showErrorMessage("Google sign-up failed: ${e.message ?: "Unknown error"}")
            }
        }
    }
}

fun monthToNumber(month: String): String {
    return when (month.lowercase()) {
        "janv." -> "01"
        "févr." -> "02"
        "mars" -> "03"
        "avr." -> "04"
        "mai" -> "05"
        "juin" -> "06"
        "juil." -> "07"
        "août" -> "08"
        "sept." -> "09"
        "oct." -> "10"
        "nov." -> "11"
        "déc." -> "12"
        else -> "01"
    }
}

