package com.example.dropwise

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface UserDao {
    @Insert
    suspend fun insert(user: User)

    @Insert
    suspend fun insertWaterIntake(waterIntake: WaterIntake)

    @Query("SELECT * FROM water_intake WHERE userId = :userId AND date = :date")
    suspend fun getWaterIntakeForDate(userId: String, date: String): WaterIntake?

    @Query("SELECT * FROM water_intake WHERE userId = :userId AND date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getWaterIntakesForWeek(userId: String, startDate: String, endDate: String): List<WaterIntake>

    @Query("SELECT * FROM users WHERE email = :email AND password = :password")
    suspend fun getUser(email: String, password: String): User?
}