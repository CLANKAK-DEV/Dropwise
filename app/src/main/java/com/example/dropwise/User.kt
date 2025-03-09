package com.example.dropwise

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String, // Changed to String to match Firebase UID
    val username: String,
    val email: String,
    val password: String = "", // Optional, can be empty
    val birthday: String = "" // Optional, can be empty
)