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
import androidx.room.Room
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class LoginActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var snackbarHostState: SnackbarHostState // Non-nullable, initialized later
    private val currentYear: Int = Calendar.getInstance().get(Calendar.YEAR)
    private val maxYear: Int = currentYear - 18

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Log.w("LoginActivity", "Google sign-in failed", e)
                showErrorMessage("Google sign-in failed: ${e.message ?: "Unknown error"}")
            }
        } else {
            showErrorMessage("Google sign-in cancelled")
        }
    }

    private fun showErrorMessage(message: String) {
        if (::snackbarHostState.isInitialized) {
            CoroutineScope(Dispatchers.Main).launch {
                snackbarHostState.showSnackbar(message)
            }
        } else {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .requestProfile()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        if (SessionManager.isLoggedIn(this)) {
            Log.d("LoginActivity", "User already logged in, skipping to MainActivity")
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContent {
            val scope = rememberCoroutineScope()
            var state by remember { mutableStateOf(LoginState()) }
            snackbarHostState = remember { SnackbarHostState() } // Initialize here

            LoginScreenUI(
                state = state,
                onValueChange = { newState -> state = newState },
                onLoginClick = {
                    loginWithEmailPassword(state.email, state.password, scope)
                },
                onGoogleSignInClick = {
                    signInWithGoogle()
                },
                onNavigateToRegister = {
                    startActivity(Intent(this@LoginActivity, RegisterActivity::class.java))
                    finish()
                },
                snackbarHostState = snackbarHostState
            )
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let {
                        handleSuccessfulGoogleAuth(user)
                    }
                } else {
                    Log.w("LoginActivity", "signInWithCredential:failure", task.exception)
                    showErrorMessage("Authentication failed: ${task.exception?.message ?: "Unknown error"}")
                }
            }
    }

    private fun loginWithEmailPassword(email: String, password: String, scope: CoroutineScope) {
        scope.launch {
            try {
                Log.d("LoginActivity", "Attempting login with email: $email")
                val db = Room.databaseBuilder(this@LoginActivity, AppDatabase::class.java, "dropwise_db")
                    .addMigrations(AppDatabase.MIGRATION_1_2)
                    .build()
                val user = withContext(Dispatchers.IO) { db.userDao().getUser(email, password) }
                if (user != null) {
                    SessionManager.login(this@LoginActivity, user.id) // user.id is String
                    snackbarHostState.showSnackbar("Login successful!")
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                } else {
                    snackbarHostState.showSnackbar("Invalid email or password")
                }
            } catch (e: Exception) {
                Log.e("LoginActivity", "Login failed: ${e.message}", e)
                snackbarHostState.showSnackbar("Login failed: ${e.message ?: "Unknown error"}")
            }
        }
    }

    private fun handleSuccessfulGoogleAuth(firebaseUser: FirebaseUser) {
        val scope = CoroutineScope(Dispatchers.Main)
        scope.launch {
            try {
                val db = Room.databaseBuilder(this@LoginActivity, AppDatabase::class.java, "dropwise_db")
                    .addMigrations(AppDatabase.MIGRATION_1_2)
                    .build()
                val email = firebaseUser.email ?: ""
                val username = firebaseUser.displayName ?: email.split("@").firstOrNull() ?: "User_${firebaseUser.uid}"
                val password = "" // No password for Google auth

                val existingUser = if (email.isNotBlank()) {
                    withContext(Dispatchers.IO) { db.userDao().getUser(email, password) }
                } else null

                if (existingUser == null) {
                    // New Google user, redirect to AgeVerificationActivity
                    val intent = Intent(this@LoginActivity, AgeVerificationActivity::class.java).apply {
                        putExtra("email", email)
                        putExtra("username", username)
                        putExtra("googleId", firebaseUser.uid)
                    }
                    startActivity(intent)
                    finish()
                } else {
                    // Existing user, log in directly
                    SessionManager.login(this@LoginActivity, firebaseUser.uid) // Use String UID
                    snackbarHostState.showSnackbar("Welcome back, $username!")
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                }
            } catch (e: Exception) {
                Log.e("LoginActivity", "Error handling Google auth: ${e.message}", e)
                snackbarHostState.showSnackbar("Error: ${e.message ?: "Unknown error"}")
            }
        }
    }
}