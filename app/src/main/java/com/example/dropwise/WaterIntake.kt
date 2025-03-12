package com.example.dropwise

import android.content.Context
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Room
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

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
    val amount: Double,
    val date: String
)
suspend fun saveWaterIntake(context: Context, amount: Double, date: String) {
    val userId = SessionManager.getUserId(context) ?: return
    val roomId = "room_$userId"

    val db = Room.databaseBuilder(context, AppDatabase::class.java, "dropwise_db")
        .addMigrations(AppDatabase.MIGRATION_1_2)
        .build()

    val waterIntake = WaterIntake(userId = userId, amount = amount, date = date)
    withContext(Dispatchers.IO) {
        db.userDao().insertWaterIntake(waterIntake)
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("rooms").document(roomId)
            .collection("water_intakes")
            .document(date)
            .set(mapOf("amount" to amount, "date" to date))
            .await()
    }
}