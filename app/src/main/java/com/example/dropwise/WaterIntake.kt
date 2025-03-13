package com.example.dropwise

import android.content.Context
import android.util.Log
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Room
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Calendar

@Entity(
    tableName = "water_intake",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class WaterIntake(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val amount: Double?,
    val date: String,
    val hour: Int? = null, // Added in MIGRATION_1_2
    val timestamp: String? = null // Added in MIGRATION_2_3
)

suspend fun saveWaterIntake(context: Context, amount: Double, date: String) {
    try {
        val userId = SessionManager.getUserId(context) ?: return
        val db = AppDatabase.getDatabase(context)
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val waterIntake = WaterIntake(userId = userId, amount = amount, date = date, hour = hour)
        db.userDao().insertWaterIntake(waterIntake)
        Log.d("GoalsScreen", "Successfully saved water intake: $amount L on $date at hour $hour")
    } catch (e: Exception) {
        Log.e("GoalsScreen", "Error saving water intake: ${e.message}", e)
        throw e // Re-throw to ensure the caller can handle the error if needed
    }
}