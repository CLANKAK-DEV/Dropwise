package com.example.dropwise


import kotlin.io.path.Path
import kotlin.io.path.moveTo
import com.example.dropwise.R
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import android.util.Log
import androidx.compose.foundation.Canvas
import kotlin.io.path.Path
import kotlin.io.path.moveTo
import kotlin.math.sin
import androidx.compose.ui.graphics.Path
class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                SplashScreen()
            }
        }
    }

    @Composable
    fun SplashScreen() {
        // Animation states
        val logoScale = remember { Animatable(0.3f) }
        var showTagline by remember { mutableStateOf(false) }
        val waveAnimation = rememberInfiniteTransition(label = "wave")
        val waveOffset = waveAnimation.animateFloat(
            initialValue = 0f,
            targetValue = 1000f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "wave motion"
        )

        // Navigation logic
        LaunchedEffect(Unit) {
            try {
                // Logo animation
                logoScale.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(1000, easing = EaseOutBack)
                )

                // Show tagline after logo animation
                showTagline = true

                // Wait for animations
                delay(1500)

                if (!SessionManager.isOnboardingCompleted(this@SplashActivity)) {
                    // First download: go to Onboarding
                    Log.d("SplashActivity", "First launch, moving to Onboarding")
                    startActivity(Intent(this@SplashActivity, OnboardingActivity::class.java))
                } else {
                    // Not first launch: check login status
                    if (SessionManager.isLoggedIn(this@SplashActivity)) {
                        Log.d("SplashActivity", "User logged in, moving to MainActivity")
                        startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                    } else {
                        Log.d("SplashActivity", "User not logged in, moving to LoginActivity")
                        startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
                    }
                }
                finish()
            } catch (e: Exception) {
                Log.e("SplashActivity", "Splash screen failed: ${e.message}", e)
                // Fallback to LoginActivity in case of error
                startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
                finish()
            }
        }

        // Gradient background with animated waves
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF2E86DE),
                            Color(0xFF54A0FF)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // Animated water waves (background decoration)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = (-50).dp)
            ) {
                for (i in 0..2) {
                    WaveLine(
                        waveOffset = waveOffset.value,
                        amplitude = 25f + (i * 5),
                        phase = i * 500f,
                        alpha = 0.2f - (i * 0.05f)
                    )
                }
            }

            // Content Column
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Logo (Water drop icon with circle background)
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(logoScale.value)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img),
                        contentDescription = "Dropwise Logo",
                        modifier = Modifier.size(64.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // App name with custom styling
                Text(
                    text = "Dropwise",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // Animated tagline
                AnimatedVisibility(
                    visible = showTagline,
                    enter = fadeIn(animationSpec = tween(500)) +
                            slideInVertically(animationSpec = tween(500)) { it }
                ) {
                    Text(
                        text = "Every drop counts",
                        fontSize = 18.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }

    @Composable

    fun WaveLine(waveOffset: Float, amplitude: Float, phase: Float, alpha: Float) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerY = height * 0.8f

            // Create the wave path
            val path = Path().apply {
                moveTo(0f, centerY) // Start point of the wave

                // Loop to generate the sine wave path
                for (x in 0..width.toInt() step 10) {
                    val y = centerY + amplitude * sin((x + waveOffset + phase) * (2f * Math.PI / 800f).toFloat())
                    lineTo(x.toFloat(), y)
                }

                // Close the path at the bottom of the canvas
                lineTo(width, height)
                lineTo(0f, height)
                close()
            }

            // Draw the path with a gradient brush
            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = alpha),
                        Color.White.copy(alpha = 0f)
                    ),
                    startY = centerY,
                    endY = centerY + 200f
                )
            )
        }
    }
}