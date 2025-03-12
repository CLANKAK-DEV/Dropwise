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
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Calendar

class LoginActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var snackbarHostState: SnackbarHostState
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
        firestore = FirebaseFirestore.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .requestProfile()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        if (SessionManager.isLoggedIn(this)) {
            Log.d("LoginActivity", "User already logged in, navigating to MainActivity")
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContent {
            val scope = rememberCoroutineScope()
            var state by remember { mutableStateOf(LoginState()) }
            snackbarHostState = remember { SnackbarHostState() }

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
        Log.d("LoginActivity", "Initiating Google sign-in")
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        Log.d("LoginActivity", "Authenticating with Google using idToken")
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("LoginActivity", "Google sign-in successful")
                    val user = auth.currentUser
                    user?.let {
                        handleSuccessfulGoogleAuth(user)
                    } ?: run {
                        Log.e("LoginActivity", "FirebaseUser is null after Google sign-in")
                        showErrorMessage("Google sign-in failed: User not found")
                    }
                } else {
                    Log.e("LoginActivity", "Google signInWithCredential failed: ${task.exception?.message}")
                    showErrorMessage("Google authentication failed: ${task.exception?.message ?: "Unknown error"}")
                }
            }
    }

    private fun loginWithEmailPassword(email: String, password: String, scope: CoroutineScope) {
        scope.launch {
            try {
                Log.d("LoginActivity", "Attempting login with email: $email")

                // Step 1: Validate input
                if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    throw Exception("Invalid email address")
                }
                if (password.isBlank()) {
                    throw Exception("Password cannot be empty")
                }

                // Step 2: Firebase Auth login
                val authResult = withContext(Dispatchers.IO) {
                    auth.signInWithEmailAndPassword(email, password).await()
                }
                val userId = authResult.user?.uid ?: throw Exception("User ID not found")
                val roomId = "room_$userId"
                Log.d("LoginActivity", "Firebase Auth successful, userId: $userId")

                // Step 3: Fetch user data from Firestore
                val userDoc = withContext(Dispatchers.IO) {
                    firestore.collection("users").document(userId).get().await()
                }
                if (!userDoc.exists()) {
                    Log.w("LoginActivity", "User document not found in Firestore, creating new entry")
                    val username = email.split("@").firstOrNull() ?: "User_$userId"
                    val userMap = hashMapOf(
                        "username" to username,
                        "email" to email,
                        "roomId" to roomId,
                        "createdAt" to System.currentTimeMillis()
                    )
                    withContext(Dispatchers.IO) {
                        firestore.collection("users").document(userId).set(userMap).await()
                    }
                }
                val userData = userDoc.data ?: mapOf(
                    "username" to (email.split("@").firstOrNull() ?: "User_$userId"),
                    "email" to email,
                    "roomId" to roomId,
                    "birthday" to ""
                )
                Log.d("LoginActivity", "User data fetched from Firestore: $userData")

                // Step 4: Sync to Room (optional)
                val db = AppDatabase.getDatabase(this@LoginActivity) // Use singleton
                val user = User(
                    id = userId,
                    username = userData["username"] as String,
                    email = userData["email"] as String,
                    password = password,
                    birthday = userData["birthday"] as String? ?: "",
                    roomId = userData["roomId"] as String? ?: roomId
                )
                withContext(Dispatchers.IO) {
                    try {
                        db.userDao().insert(user)
                        Log.d("LoginActivity", "User successfully saved to Room")
                    } catch (e: Exception) {
                        Log.e("LoginActivity", "Failed to save user to Room: ${e.message}", e)
                        // Proceed even if Room fails
                    }
                }

                // Step 5: Ensure room exists in Firestore
                val roomDoc = withContext(Dispatchers.IO) {
                    firestore.collection("rooms").document(roomId).get().await()
                }
                if (!roomDoc.exists()) {
                    withContext(Dispatchers.IO) {
                        firestore.collection("rooms").document(roomId).set(
                            mapOf(
                                "userId" to userId,
                                "createdAt" to System.currentTimeMillis()
                            )
                        ).await()
                    }
                    Log.d("LoginActivity", "Room created in Firestore: $roomId")
                }

                // Step 6: Login and Navigate
                SessionManager.login(this@LoginActivity, userId)
                Log.d("LoginActivity", "User logged in via SessionManager, navigating to MainActivity")
                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                finish()

            } catch (e: Exception) {
                Log.e("LoginActivity", "Login failed: ${e.message}", e)
                when {
                    e.message?.contains("wrong-password") == true -> {
                        showErrorMessage("Invalid password. Please try again.")
                    }
                    e.message?.contains("user-not-found") == true -> {
                        showErrorMessage("User not found. Please register first.")
                    }
                    e.message?.contains("network") == true -> {
                        showErrorMessage("Network error. Please check your internet connection.")
                    }
                    else -> {
                        showErrorMessage("Login failed: ${e.message ?: "Unknown error"}")
                    }
                }
            }
        }
    }

    private fun handleSuccessfulGoogleAuth(firebaseUser: FirebaseUser) {
        val scope = CoroutineScope(Dispatchers.Main)
        scope.launch {
            try {
                val userId = firebaseUser.uid
                val email = firebaseUser.email ?: ""
                val username = firebaseUser.displayName ?: email.split("@").firstOrNull() ?: "User_$userId"
                val roomId = "room_$userId"
                Log.d("LoginActivity", "Handling Google auth for userId: $userId")

                // Step 1: Check or create user in Firestore
                val userDoc = withContext(Dispatchers.IO) {
                    firestore.collection("users").document(userId).get().await()
                }
                if (!userDoc.exists()) {
                    Log.w("LoginActivity", "Google user not found in Firestore, creating new entry")
                    val userMap = hashMapOf(
                        "username" to username,
                        "email" to email,
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
                }
                val userData = userDoc.data ?: mapOf(
                    "username" to username,
                    "email" to email,
                    "roomId" to roomId,
                    "birthday" to ""
                )
                Log.d("LoginActivity", "Google user data: $userData")

                // Step 2: Sync to Room (optional)
                val db = AppDatabase.getDatabase(this@LoginActivity)
                val user = User(
                    id = userId,
                    username = userData["username"] as String,
                    email = userData["email"] as String,
                    birthday = userData["birthday"] as String? ?: "",
                    roomId = userData["roomId"] as String? ?: roomId
                )
                withContext(Dispatchers.IO) {
                    try {
                        db.userDao().insert(user)
                        Log.d("LoginActivity", "Google user synced to Room")
                    } catch (e: Exception) {
                        Log.e("LoginActivity", "Failed to sync Google user to Room: ${e.message}", e)
                        // Proceed even if Room fails
                    }
                }

                // Step 3: Login and Navigate
                SessionManager.login(this@LoginActivity, userId)
                Log.d("LoginActivity", "Google user logged in, navigating to MainActivity")
                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                finish()

            } catch (e: Exception) {
                Log.e("LoginActivity", "Google auth failed: ${e.message}", e)
                showErrorMessage("Google login failed: ${e.message ?: "Unknown error"}")
            }
        }
    }
}