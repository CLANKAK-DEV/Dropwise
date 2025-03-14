package com.example.dropwise

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface UserDao {
    @Insert
    suspend fun insert(user: User)
    @Update
    suspend fun updateUser(user: User)

    @Query("DELETE FROM water_intake WHERE userId = :userId") // Corrected column name
    suspend fun deleteWaterIntakesByUserId(userId: String)

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUser(userId: String): User?
    @Insert
    suspend fun insertWaterIntake(waterIntake: WaterIntake)

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: String): User?

    @Query("SELECT * FROM water_intake WHERE userId = :userId AND date = :date")
    suspend fun getWaterIntakeForDate(userId: String, date: String): WaterIntake?

    @Query("SELECT * FROM water_intake WHERE userId = :userId AND date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getWaterIntakesForWeek(userId: String, startDate: String, endDate: String): List<WaterIntake>

    @Query("SELECT * FROM users WHERE email = :email AND password = :password")
    suspend fun getUser(email: String, password: String): User?

    @Query("SELECT * FROM water_intake WHERE userId = :userId AND date BETWEEN :startDate AND :endDate")
    suspend fun getWaterIntakesForRange(userId: String, startDate: String, endDate: String): List<WaterIntake>

    @Query("SELECT SUM(amount) FROM water_intake WHERE userId = :userId AND date = :date AND strftime('%H', timestamp) = printf('%02d', :hour) AND timestamp IS NOT NULL")
    suspend fun getTotalWaterIntakeForHour(userId: String, date: String, hour: Int): Double?

    // Debug query to fetch all entries for a day
    @Query("SELECT * FROM water_intake WHERE userId = :userId AND date = :date")
    suspend fun getAllWaterIntakesForDay(userId: String, date: String): List<WaterIntake>


}