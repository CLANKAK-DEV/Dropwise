package com.example.dropwise

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AccountScreen() { // Changed to a Composable function
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()
    val user = auth.currentUser
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var username by remember { mutableStateOf("User") }
    var email by remember { mutableStateOf(user?.email ?: "N/A") }
    var age by remember { mutableStateOf("N/A") }
    var phoneNumber by remember { mutableStateOf("") }
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var isEditingPhone by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            profileImageUri = it
            user?.uid?.let { uid ->
                scope.launch {
                    uploadProfileImage(it, uid, storage)
                }
            }
        }
    }

    LaunchedEffect(user) {
        if (user != null) {
            firestore.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        username = document.getString("username") ?: "User"
                        val birthday = document.getString("birthday")
                        phoneNumber = document.getString("phoneNumber") ?: ""
                        birthday?.let {
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            val birthDate = sdf.parse(it)
                            val calendar = Calendar.getInstance()
                            calendar.time = birthDate
                            val birthYear = calendar.get(Calendar.YEAR)
                            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                            age = (currentYear - birthYear).toString()
                        }
                        storage.reference.child("profile_images/${user.uid}").downloadUrl
                            .addOnSuccessListener { uri ->
                                profileImageUri = uri
                            }
                    }
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Mon compte",
            fontSize = 24.sp,
            color = Color(0xFF333333),
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Color.LightGray)
                .clickable { imagePicker.launch("image/*") }
                .border(2.dp, Color(0xFF4A90E2), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (profileImageUri != null) {
                Text(text = "Image Loaded", color = Color.White, fontSize = 16.sp) // Replace with image display later
            } else {
                Text(text = "Ajouter Photo", color = Color(0xFF4A90E2), fontSize = 16.sp)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Nom d'utilisateur: $username", color = Color(0xFF333333), fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Email: $email",
                        color = Color(0xFF333333),
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        val intent = Intent(context, UpdateEmailActivity::class.java)
                        context.startActivity(intent)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Email",
                            tint = Color(0xFF4A90E2)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Âge: $age ans", color = Color(0xFF333333), fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                if (isEditingPhone) {
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Numéro de téléphone") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (user != null) {
                                scope.launch {
                                    firestore.collection("users").document(user.uid)
                                        .update("phoneNumber", phoneNumber)
                                        .await()
                                    isEditingPhone = false
                                }
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Enregistrer")
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Numéro de téléphone: ${phoneNumber.ifEmpty { "Non défini" }}",
                            color = Color(0xFF333333),
                            fontSize = 16.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { isEditingPhone = true }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Phone",
                                tint = Color(0xFF4A90E2)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                auth.signOut()
                SessionManager.logout(context) // Assuming SessionManager is defined elsewhere
                context.startActivity(Intent(context, LoginActivity::class.java))
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90E2)),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(text = "Déconnexion", color = Color.White, fontSize = 18.sp)
        }
    }
}

suspend fun uploadProfileImage(uri: Uri, userId: String, storage: FirebaseStorage) {
    val storageRef = storage.reference.child("profile_images/$userId")
    storageRef.putFile(uri)
        .addOnSuccessListener {
            // Image uploaded successfully
        }
        .addOnFailureListener { e ->
            println("Image upload failed: ${e.message}")
        }
}

