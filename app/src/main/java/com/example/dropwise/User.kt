package com.example.dropwise

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String,
    val username: String,
    val email: String,
    val password: String = "", // Add password field
     val birthday: String = "",
    val roomId: String = "room_$id" // Ensure this is part of the schema
)