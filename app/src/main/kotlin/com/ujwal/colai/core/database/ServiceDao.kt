package com.ujwal.colai.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ujwal.colai.core.model.AIService
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [AIService] entities.
 */
@Dao
interface ServiceDao {

    @Query("SELECT * FROM ai_services ORDER BY sortOrder ASC, createdAt ASC")
    fun getAllServices(): Flow<List<AIService>>

    @Query("SELECT * FROM ai_services ORDER BY sortOrder ASC, createdAt ASC")
    suspend fun getAllServicesList(): List<AIService>

    @Query("SELECT * FROM ai_services WHERE id = :id LIMIT 1")
    fun getServiceById(id: String): Flow<AIService?>

    @Query("SELECT * FROM ai_services WHERE id = :id LIMIT 1")
    suspend fun getServiceByIdSync(id: String): AIService?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertService(service: AIService)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServices(services: List<AIService>)

    @Update
    suspend fun updateService(service: AIService)

    @Delete
    suspend fun deleteService(service: AIService)

    @Query("DELETE FROM ai_services WHERE id = :id")
    suspend fun deleteServiceById(id: String)

    @Query("SELECT COUNT(*) FROM ai_services")
    suspend fun countServices(): Int
}
