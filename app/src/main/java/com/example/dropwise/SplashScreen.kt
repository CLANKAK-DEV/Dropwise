package com.example.dropwise

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import android.util.Log

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SplashScreen()
        }
    }

    @Composable
    fun SplashScreen() {
        LaunchedEffect(Unit) {
            try {
                // Show splash for 2 seconds
                delay(2000)
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF4A90E2)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Dropwise",
                fontSize = 32.sp,
                color = Color.White
            )
        }
    }
}