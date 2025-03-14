package com.example.dropwise

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditAccountActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                EditAccountScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAccountScreen() {
    val context = LocalContext.current as ComponentActivity // Cast to ComponentActivity
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser
    val scope = rememberCoroutineScope()

    // State variables for user data
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(user?.email ?: "N/A") }
    var birthday by remember { mutableStateOf("") }
    var profileImageResId by remember { mutableStateOf(R.drawable.dropwiselogo) } // Default profile image
    var loadingComplete by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Fetch user data when the screen loads
    LaunchedEffect(user) {
        if (user != null) {
            try {
                val db = AppDatabase.getDatabase(context)
                val userDao = db.userDao()
                val userData = withContext(Dispatchers.IO) {
                    userDao.getUserById(user.uid)
                }
                userData?.let {
                    username = it.username
                    email = it.email
                    birthday = it.birthday
                } ?: run {
                    username = ""
                    email = user.email ?: "N/A"
                    birthday = ""
                    profileImageResId = R.drawable.dropwiselogo
                }
            } catch (e: Exception) {
                println("Failed to fetch user data from Room: ${e.message}")
            }
            kotlinx.coroutines.delay(500)
            loadingComplete = true
        }
    }

    // Parse initial birthday for the date picker
    val initialDate = parseBirthday(birthday)
    val initialDay = initialDate.first
    val initialMonth = initialDate.second
    val initialYear = initialDate.third

    // Show DatePickerDialog when triggered
    if (showDatePicker) {
        DatePickerDialog(
            initialDay = initialDay,
            initialMonth = initialMonth,
            initialYear = initialYear,
            maxYear = 2025,
            onConfirm = { day, monthAbbreviation, year ->
                val monthNumber = monthToNumber(monthAbbreviation)
                birthday = "$day-$monthNumber-$year"
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Account", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { context.finish() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.Black
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black,
                    navigationIconContentColor = Color.Black
                )
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile image section
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF5F5F5))
                        .clickable {
                            profileImageResId = if (profileImageResId == R.drawable.dropwiselogo) {
                                R.drawable.ic_launcher_foreground
                            } else {
                                R.drawable.dropwiselogo
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = profileImageResId),
                        contentDescription = "Profile Image",
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                    )
                    Text(
                        text = "Change",
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Username field
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color(0xFF4A90E2),
                        unfocusedIndicatorColor = Color.LightGray
                    )
                )

                // Email field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color(0xFF4A90E2),
                        unfocusedIndicatorColor = Color.LightGray
                    )
                )

                // Birthday field with date picker
                OutlinedTextField(
                    value = birthday,
                    onValueChange = { /* Read-only field, handled by date picker */ },
                    label = { Text("Birthday") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { showDatePicker = true },
                    enabled = false,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color(0xFF4A90E2),
                        unfocusedIndicatorColor = Color.LightGray,
                        disabledIndicatorColor = Color.LightGray,
                        disabledTextColor = Color(0xFF333333)
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Save button
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                val db = AppDatabase.getDatabase(context)
                                val userDao = db.userDao()
                                val updatedUser = User(
                                    id = user?.uid ?: "",
                                    username = username,
                                    email = email,
                                    birthday = birthday,
                                )
                                withContext(Dispatchers.IO) {
                                    userDao.updateUser(updatedUser)
                                }
                                // Optionally update Firebase email if changed
                                user?.updateEmail(email)?.addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        println("Email updated in Firebase")
                                    } else {
                                        println("Failed to update email in Firebase: ${task.exception?.message}")
                                    }
                                }
                                // Set result to indicate data update and navigate back
                                context.setResult(Activity.RESULT_OK, Intent().putExtra("refresh", true))
                                context.startActivity(Intent(context, AccountActivity::class.java))
                                context.finish()
                            } catch (e: Exception) {
                                println("Failed to save user data: ${e.message}")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4A90E2)
                    )
                ) {
                    Text(
                        text = "Save Changes",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    )
}




private fun parseBirthday(birthday: String): Triple<Int, Int, Int> {
    // Expected format: DD-MM-YYYY
    return if (birthday.isNotEmpty() && birthday.matches(Regex("\\d{2}-\\d{2}-\\d{4}"))) {
        val parts = birthday.split("-")
        val day = parts[0].toIntOrNull() ?: 1
        val month = parts[1].toIntOrNull() ?: 1
        val year = parts[2].toIntOrNull() ?: 1980
        Triple(day, month, year)
    } else {
        Triple(1, 1, 1980) // Default to 01-01-1980 if invalid
    }
}

