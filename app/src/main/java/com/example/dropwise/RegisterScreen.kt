package com.example.dropwise

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
                snackbarHostState = snackbarHostState
            )
        }
    }

    private fun registerWithEmailPassword(state: RegistrationState, scope: CoroutineScope, snackbarHostState: SnackbarHostState) {
        scope.launch {
            try {
                // Step 1: Validation
                if (state.username.isBlank()) throw Exception("Username is required")
                if (state.email.isBlank() || !isEmailValid(state.email)) throw Exception("Invalid email")
                if (state.password.isBlank()) throw Exception("Password is required")
                if (state.day.isBlank() || state.month.isBlank() || state.year.isBlank()) throw Exception("Date of birth is required")

                val birthday = "${state.year}-${monthToNumber(state.month)}-${state.day.padStart(2, '0')}"
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                sdf.isLenient = false
                val birthDate = sdf.parse(birthday) ?: throw Exception("Invalid date of birth")
                val age = currentYear - SimpleDateFormat("yyyy", Locale.getDefault()).format(birthDate).toInt()
                if (age < 18) throw Exception("You must be at least 18 years old")

                // Step 2: Register with Firebase Auth
                val authResult = withContext(Dispatchers.IO) {
                    auth.createUserWithEmailAndPassword(state.email, state.password).await()
                }
                val userId = authResult.user?.uid ?: throw Exception("User ID not found")
                val roomId = "room_$userId"

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

                // Step 4: Save to Room
                try {
                    val db = AppDatabase.getDatabase(this@RegisterActivity)
                    val user = User(
                        id = userId,
                        username = state.username,
                        email = state.email,
                        password = state.password, // Hash in production!
                        birthday = birthday,
                        roomId = roomId
                    )
                    withContext(Dispatchers.IO) {
                        db.userDao().insert(user)
                    }
                } catch (e: Exception) {
                    Log.e("RegisterActivity", "Failed to save to Room: ${e.message}", e)
                    // Optionally, proceed even if Room fails since Firestore succeeded
                    snackbarHostState.showSnackbar("Local storage failed, but account created.")
                }

                // Step 5: Login and Navigate
                SessionManager.login(this@RegisterActivity, userId)
                snackbarHostState.showSnackbar("Registration successful! Redirecting to Dashboard.")
                startActivity(Intent(this@RegisterActivity, MainActivity::class.java))
                finish()

            } catch (e: Exception) {
                Log.e("RegisterActivity", "Registration failed: ${e.message}", e)
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
                // Step 1: Authenticate with Firebase
                val authResult = withContext(Dispatchers.IO) {
                    auth.signInWithCredential(credential).await()
                }
                val userId = authResult.user?.uid ?: throw Exception("User ID not found")
                val roomId = "room_$userId"

                // Step 2: Save to Firestore
                val userMap = hashMapOf(
                    "username" to (account.displayName ?: "Google User"),
                    "email" to (account.email ?: ""),
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

                // Step 3: Save to Room
                try {
                    val db = AppDatabase.getDatabase(this@RegisterActivity)
                    val user = User(
                        id = userId,
                        username = account.displayName ?: "Google User",
                        email = account.email ?: "",
                        roomId = roomId
                    )
                    withContext(Dispatchers.IO) {
                        db.userDao().insert(user)
                    }
                } catch (e: Exception) {
                    Log.e("RegisterActivity", "Failed to save to Room: ${e.message}", e)
                    showErrorMessage("Local storage failed, but account created.")
                }

                // Step 4: Login and Navigate
                SessionManager.login(this@RegisterActivity, userId)
                startActivity(Intent(this@RegisterActivity, MainActivity::class.java))
                finish()

            } catch (e: Exception) {
                Log.e("RegisterActivity", "Google sign-up failed: ${e.message}", e)
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
