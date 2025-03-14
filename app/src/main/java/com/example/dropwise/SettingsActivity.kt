package com.example.dropwise

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.mutableStateMapOf
import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.EmailAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class SettingsActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance() // Initialize FirebaseAuth
        setContent {
            MaterialTheme {
                SettingsScreen()
            }
        }
    }
}
// TipsScreen Activity
class TipsScreenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                TipsScreen(this) // Pass the activity to TipsScreen
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope() // Define coroutine scope
    var showEditAccount by remember { mutableStateOf(false) }
    var showPrivacySecurity by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }

    val prefs = context.getSharedPreferences("DropwisePrefs", Context.MODE_PRIVATE)
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showSessionTimeoutDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var backupEnabled by remember {
        mutableStateOf(prefs.getBoolean("backupEnabled", false))
    }
    var sessionTimeout by remember {
        mutableStateOf(prefs.getInt("sessionTimeout", 30))
    }

    val drunkCups = remember { mutableStateMapOf<String, Boolean>() }
    val schedule = listOf(
        Pair(8 * 3600000L, 0),
        Pair(12 * 3600000L, 1),
        Pair(16 * 3600000L, 2)
    )

    fun collapseAll() {
        showEditAccount = false
        showPrivacySecurity = false
    }

    fun clearUserData() {
        val userId = SessionManager.getUserId(context) ?: return

        CoroutineScope(Dispatchers.IO).launch {
            val db = Room.databaseBuilder(context, AppDatabase::class.java, "dropwise_db").build()

            withContext(Dispatchers.IO) {
                db.userDao().deleteWaterIntakesByUserId(userId)
                with(prefs.edit()) {
                    remove("inputValue")
                    remove("inputType")
                    remove("dailyWaterGoal")
                    remove("waterPerHour")
                    remove("cupsPerDay")
                    remove("progress")
                    (0 until prefs.getInt("schedule_size", 0)).forEach { i ->
                        remove("schedule_time_$i")
                        remove("schedule_amount_$i")
                        remove("drunk_${prefs.getString("schedule_time_$i", "")}")
                    }
                    remove("schedule_size")
                    remove("globalAlarmActive")
                    remove("dataSharingEnabled")
                    remove("sessionTimeout")
                    remove("backupEnabled")
                    apply()
                }
                Log.d("SettingsScreen", "Cleared user data for userId: $userId")
            }

            withContext(Dispatchers.Main) {
                val activity = context as? Activity
                if (activity != null) {
                    context.startActivity(Intent(context, MainActivity::class.java))
                    activity.finish()
                } else {
                    Log.e("SettingsScreen", "Context is not an Activity!")
                }
            }
        }
    }

    suspend fun deleteAccount(password: String) = withContext(Dispatchers.IO) {
        try {
            val user = SessionManager.getCurrentUser()?.currentUser // Get the current FirebaseUser
            if (user != null) {
                val credential = EmailAuthProvider.getCredential(user.email ?: "", password)

                try {
                    // Re-authenticate the user
                    user.reauthenticate(credential).await() // Await for reauthentication to complete

                    // Perform deletion of water intakes in the background
                    val db = Room.databaseBuilder(context, AppDatabase::class.java, "dropwise_db").build()
                    db.userDao().deleteWaterIntakesByUserId(user.uid)

                    // Clear preferences
                    with(prefs.edit()) {
                        clear()
                        apply()
                    }

                    // Delete user account
                    user.delete().await() // Await for account deletion

                    // Handle success and perform UI updates on the main thread
                    withContext(Dispatchers.Main) {
                        Log.d("SettingsScreen", "Account deleted successfully")
                        SessionManager.logout(context) // Log out the user
                        val activity = context as? Activity
                        activity?.let {
                            context.startActivity(Intent(context, LoginActivity::class.java))
                            it.finish()
                        }
                    }
                } catch (e: Exception) {
                    // Handle reauthentication or deletion failure
                    Log.e("SettingsScreen", "Error during account deletion", e)
                }
            } else {
                Log.e("SettingsScreen", "No authenticated user found")
            }
        } catch (e: Exception) {
            Log.e("SettingsScreen", "Delete account failed", e)
        }
    }
    fun performBackup() {
        Log.d("SettingsScreen", "Backup performed (placeholder)")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { (context as? ComponentActivity)?.finish() }) {
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
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFF5F7FA),
                                Color(0xFFE8ECEF)
                            )
                        )
                    )
            ) {
                SettingsItem(
                    icon = Icons.Outlined.Person,
                    title = "Account",
                    onClick = {
                        showEditAccount = !showEditAccount
                        if (showEditAccount) showPrivacySecurity = false
                    }
                )
                AnimatedVisibility(
                    visible = showEditAccount,
                    enter = fadeIn(animationSpec = tween(300)),
                    exit = fadeOut(animationSpec = tween(300))
                ) {
                    LazyColumn {
                        item {
                            SettingsSubItem(
                                icon = Icons.Outlined.Edit,
                                title = "Edit Account",
                                onClick = {
                                    context.startActivity(Intent(context, EditAccountActivity::class.java))
                                }
                            )
                        }
                        item {
                            SettingsSubItem(
                                icon = Icons.Outlined.Delete,
                                title = "Delete Account",
                                onClick = { showDeleteAccountDialog = true }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                SettingsItem(
                    icon = Icons.Outlined.Lock,
                    title = "Privacy & Security",
                    onClick = {
                        showPrivacySecurity = !showPrivacySecurity
                        if (showPrivacySecurity) showEditAccount = false
                    }
                )
                AnimatedVisibility(
                    visible = showPrivacySecurity,
                    enter = fadeIn(animationSpec = tween(300)),
                    exit = fadeOut(animationSpec = tween(300))
                ) {
                    LazyColumn {
                        item {
                            SettingsSubItem(
                                icon = Icons.Outlined.Delete,
                                title = "Clear App Data",
                                onClick = { showClearDataDialog = true }
                            )
                        }
                        item {
                            SettingsSubItem(
                                icon = Icons.Outlined.Timer,
                                title = "Session Timeout",
                                onClick = { showSessionTimeoutDialog = true }
                            ) {
                                Text(
                                    text = "${sessionTimeout} min",
                                    fontSize = 14.sp,
                                    color = Color(0xFF4A90E2)
                                )
                            }
                        }
                        item {
                            SettingsSubItem(
                                icon = Icons.Outlined.Backup,
                                title = "Backup and Encryption",
                                onClick = { showBackupDialog = true }
                            ) {
                                Switch(
                                    checked = backupEnabled,
                                    onCheckedChange = { newValue ->
                                        backupEnabled = newValue
                                        with(prefs.edit()) {
                                            putBoolean("backupEnabled", backupEnabled)
                                            apply()
                                        }
                                        if (backupEnabled) {
                                            performBackup()
                                        }
                                    },
                                    colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF7ED321))
                                )
                            }
                        }
                        item {
                            SettingsSubItem(
                                icon = Icons.Outlined.Info,
                                title = "Privacy Policy",
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        data = Uri.parse("https://privacy-policy-drop-wise.vercel.app/")
                                    }
                                    context.startActivity(intent)
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                // Modify the Help and Support SettingsItem
                SettingsItem(
                    icon = Icons.Outlined.HelpOutline,
                    title = "Help and Support",
                    onClick = {
                        collapseAll()
                        context.startActivity(Intent(context, TipsScreenActivity::class.java))
                    }
                )
                Spacer(modifier = Modifier.height(2.dp))
                SettingsItem(
                    icon = Icons.Outlined.Info,
                    title = "About",
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://lahoucines.gitbook.io/dropwise/~/changes/1"))
                        context.startActivity(intent)
                    }
                )
            }
        }
    )

    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("Clear User Data") },
            text = { Text("Are you sure you want to clear your personal data? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        clearUserData()
                        showClearDataDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90E2))
                ) {
                    Text("Clear", color = Color.White)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showClearDataDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEEEEEE))
                ) {
                    Text("Cancel", color = Color.Black)
                }
            }
        )
    }

    if (showSessionTimeoutDialog) {
        val timeoutOptions = listOf(15, 30, 60, 120)
        AlertDialog(
            onDismissRequest = { showSessionTimeoutDialog = false },
            title = { Text("Select Session Timeout") },
            text = {
                Column {
                    timeoutOptions.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    sessionTimeout = option
                                    with(prefs.edit()) {
                                        putInt("sessionTimeout", sessionTimeout)
                                        apply()
                                    }
                                    showSessionTimeoutDialog = false
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = sessionTimeout == option,
                                onClick = null
                            )
                            Text(text = "$option minutes", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSessionTimeoutDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEEEEEE))
                ) {
                    Text("Cancel", color = Color.Black)
                }
            }
        )
    }

    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text("Backup and Encryption") },
            text = { Text("Do you want to perform a backup now? (Encryption placeholder)") },
            confirmButton = {
                Button(
                    onClick = {
                        performBackup()
                        showBackupDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90E2))
                ) {
                    Text("Backup", color = Color.White)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showBackupDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEEEEEE))
                ) {
                    Text("Cancel", color = Color.Black)
                }
            }
        )
    }

    // Delete Account Dialog
    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text("Confirm Delete Account") },
            text = {
                Column {
                    Text("This action will permanently delete your account. Please enter your password to confirm:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            deleteAccount(password)
                            showDeleteAccountDialog = false
                            password = "" // Clear password after attempt
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90E2))
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showDeleteAccountDialog = false
                        password = "" // Clear password on cancel
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEEEEEE))
                ) {
                    Text("Cancel", color = Color.Black)
                }
            }
        )
    }
}


@Composable
fun SettingsItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.run {
            fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .clickable(onClick = onClick) // onClick is now a simple lambda function
        },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF333333),
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF333333),
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ArrowForwardIos,
                contentDescription = "Arrow",
                tint = Color(0xFF666666),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun SettingsSubItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF8F9FA))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(4.dp)
                .background(Color(0xFF4A90E2))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF4A90E2),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF4A90E2),
            modifier = Modifier.weight(1f)
        )
        content()
        Icon(
            imageVector = Icons.Default.ArrowForwardIos,
            contentDescription = "Arrow",
            tint = Color(0xFF4A90E2),
            modifier = Modifier.size(14.dp)
        )
    }
}

