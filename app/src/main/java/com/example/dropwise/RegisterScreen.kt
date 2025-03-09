package com.example.dropwise

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.room.Room
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
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

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                showErrorMessage("Google sign-up failed: ${e.statusCode}")
            }
        } else {
            showErrorMessage("Google sign-up failed")
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
                snackbarHostState = snackbarHostState
            )
        }
    }

    private fun registerWithEmailPassword(state: RegistrationState, scope: CoroutineScope, snackbarHostState: SnackbarHostState) {
        scope.launch {
            try {
                // Rely on UI validation for most checks, but confirm critical ones here
                if (state.username.isBlank()) {
                    snackbarHostState.showSnackbar("Username is required")
                    return@launch
                }

                if (state.email.isBlank() || !isEmailValid(state.email)) {
                    snackbarHostState.showSnackbar(
                        if (state.email.isBlank()) "Email is required" else "Invalid email format"
                    )
                    return@launch
                }

                if (state.password.isBlank()) {
                    snackbarHostState.showSnackbar("Password is required")
                    return@launch
                }

                if (state.day.isBlank() || state.month.isBlank() || state.year.isBlank()) {
                    snackbarHostState.showSnackbar("Date of birth is required")
                    return@launch
                }

                // Format and validate date of birth
                val birthday = "${state.year}-${monthToNumber(state.month)}-${state.day.padStart(2, '0')}"
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                sdf.isLenient = false
                val birthDate = try {
                    withContext(Dispatchers.IO) { sdf.parse(birthday) }
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Invalid date of birth")
                    return@launch
                }

                val calendar = Calendar.getInstance()
                calendar.time = birthDate
                val birthYear = calendar.get(Calendar.YEAR)
                val age = currentYear - birthYear
                if (age < 18) {
                    snackbarHostState.showSnackbar("You must be at least 18 years old")
                    return@launch
                }

                // Register with Firebase Authentication
                val authResult = withContext(Dispatchers.IO) {
                    auth.createUserWithEmailAndPassword(state.email, state.password).await()
                }
                val userId = authResult.user?.uid ?: throw Exception("User ID not found")

                // Initialize Room database
                val db = Room.databaseBuilder(this@RegisterActivity, AppDatabase::class.java, "dropwise_db")
                    .addMigrations(AppDatabase.MIGRATION_1_2)
                    .build()

                // Save to Room
                val user = User(
                    id = userId,
                    username = state.username,
                    email = state.email,
                    password = state.password, // Note: Hash this in production!
                    birthday = birthday
                )
                withContext(Dispatchers.IO) {
                    db.userDao().insert(user)
                }

                // Save to Firestore
                val userMap = hashMapOf(
                    "username" to state.username,
                    "email" to state.email,
                    "birthday" to birthday,
                    "createdAt" to System.currentTimeMillis()
                )
                withContext(Dispatchers.IO) {
                    firestore.collection("users")
                        .document(userId)
                        .set(userMap)
                        .await()
                }

                SessionManager.login(this@RegisterActivity, userId)
                snackbarHostState.showSnackbar("Registration successful! Redirecting to Dashboard.")
                startActivity(Intent(this@RegisterActivity, MainActivity::class.java))
                finish()

            } catch (e: Exception) {
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

                // Save Google user data to Firestore
                val userMap = hashMapOf(
                    "username" to (account.displayName ?: "Google User"),
                    "email" to (account.email ?: ""),
                    "createdAt" to System.currentTimeMillis()
                )
                withContext(Dispatchers.IO) {
                    firestore.collection("users")
                        .document(userId)
                        .set(userMap)
                        .await()
                }

                // Redirect to AgeVerificationActivity
                val intent = Intent(this@RegisterActivity, AgeVerificationActivity::class.java).apply {
                    putExtra("email", account.email ?: "")
                    putExtra("username", account.displayName ?: "Google User")
                    putExtra("googleId", account.id ?: "")
                }
                startActivity(intent)
                finish()

            } catch (e: Exception) {
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