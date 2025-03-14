package com.example.dropwise

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AccountActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AccountScreen()
            }
        }
    }
}

@Composable
fun AccountScreen() {
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(user?.email ?: "N/A") }
    var birthday by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var loadingComplete by remember { mutableStateOf(false) }

    val profileScale = animateFloatAsState(
        targetValue = if (loadingComplete) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )

    val headerAlpha = animateFloatAsState(
        targetValue = if (loadingComplete) 1f else 0f,
        animationSpec = tween(durationMillis = 800)
    )

    val menuButtonScale by animateFloatAsState(
        targetValue = if (showMenu) 1.2f else 1f,
        animationSpec = tween(durationMillis = 200)
    )

    val infiniteTransition = rememberInfiniteTransition(label = "borderAnimation")
    val borderRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "borderRotation"
    )

    val gradientColors = listOf(
        Color(0xFF4A90E2),
        Color(0xFF6FCF97),
        Color(0xFF4A90E2)
    )

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
                }
            } catch (e: Exception) {
                println("Failed to fetch user data from Room: ${e.message}")
            }
            kotlinx.coroutines.delay(500)
            loadingComplete = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF5F8FF),
                        Color(0xFFFFFFFF)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .graphicsLayer(alpha = headerAlpha.value),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "My Profile",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333),
                    style = MaterialTheme.typography.headlineMedium
                )
                Box {
                    IconButton(
                        onClick = { showMenu = !showMenu },
                        modifier = Modifier.scale(menuButtonScale)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = Color(0xFF333333)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = null,
                                        tint = Color(0xFF4A90E2),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Settings")
                                }
                            },
                            onClick = {
                                showMenu = false
                                // Navigate to SettingsActivity
                                context.startActivity(Intent(context, SettingsActivity::class.java))
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ExitToApp,
                                        contentDescription = null,
                                        tint = Color(0xFF4A90E2),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Log out")
                                }
                            },
                            onClick = {
                                showMenu = false
                                auth.signOut()
                                SessionManager.logout(context)
                                context.startActivity(Intent(context, LoginActivity::class.java))
                            }
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(140.dp)
                    .graphicsLayer {
                        scaleX = profileScale.value
                        scaleY = profileScale.value
                        rotationZ = borderRotation
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.sweepGradient(gradientColors)
                        )
                )

                Box(
                    modifier = Modifier
                        .size(132.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.dropwiselogo),
                        contentDescription = "Default Profile Image",
                        modifier = Modifier
                            .size(128.dp)
                            .clip(CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(
                visible = loadingComplete,
                enter = fadeIn(animationSpec = tween(500)) +
                        slideInVertically(animationSpec = tween(500)) { it / 2 },
                exit = fadeOut() + slideOutVertically()
            ) {
                Text(
                    text = if (username.isEmpty()) "Set username" else "@$username",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF4A90E2)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            AnimatedVisibility(
                visible = loadingComplete,
                enter = fadeIn(animationSpec = tween(800, delayMillis = 300)) +
                        slideInVertically(animationSpec = tween(800)) { it / 2 },
                exit = fadeOut() + slideOutVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    ProfileInfoCard(
                        icon = Icons.Outlined.Person,
                        label = "USERNAME",
                        value = if (username.isEmpty()) "Not set" else username,
                        showEdit = false
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    ProfileInfoCard(
                        icon = Icons.Outlined.Email,
                        label = "EMAIL",
                        value = email,
                        showEdit = false
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    ProfileInfoCard(
                        icon = Icons.Outlined.CalendarMonth,
                        label = "BIRTH DATE",
                        value = if (birthday.isEmpty()) "Not set" else birthday,
                        showEdit = false
                    )
                }
            }
        }
    }
}


// Existing ProfileInfoCard remains the same
@Composable
fun ProfileInfoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    showEdit: Boolean = false,
    onEditClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF4A90E2),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = value,
                    fontSize = 16.sp,
                    color = Color(0xFF333333),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}