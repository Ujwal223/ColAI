package com.ujwal.colai.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ujwal.colai.core.model.Session
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [Session] entities.
 */
@Dao
interface SessionDao {

    @Query("SELECT * FROM sessions WHERE serviceId = :serviceId ORDER BY isDefault DESC, lastAccessed DESC")
    fun getSessionsForService(serviceId: String): Flow<List<Session>>

    @Query("SELECT * FROM sessions WHERE serviceId = :serviceId ORDER BY isDefault DESC, lastAccessed DESC")
    suspend fun getSessionsForServiceList(serviceId: String): List<Session>

    @Query("SELECT * FROM sessions ORDER BY lastAccessed DESC")
    fun getAllSessions(): Flow<List<Session>>

    @Query("SELECT * FROM sessions ORDER BY lastAccessed DESC")
    suspend fun getAllSessionsList(): List<Session>

    @Query("SELECT * FROM sessions WHERE id = :id LIMIT 1")
    fun getSessionById(id: String): Flow<Session?>

    @Query("SELECT * FROM sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionByIdSync(id: String): Session?

    @Query("SELECT * FROM sessions WHERE serviceId = :serviceId AND isDefault = 1 LIMIT 1")
    suspend fun getDefaultSessionForService(serviceId: String): Session?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: Session)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<Session>)

    @Update
    suspend fun updateSession(session: Session)

    @Delete
    suspend fun deleteSession(session: Session)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteSessionById(id: String)

    @Query("DELETE FROM sessions")
    suspend fun deleteAllSessions()

    @Query("UPDATE sessions SET isDefault = 0 WHERE serviceId = :serviceId")
    suspend fun clearDefaultFlagsForService(serviceId: String)

    @Query("UPDATE sessions SET lastAccessed = :timestamp WHERE id = :sessionId")
    suspend fun updateLastAccessed(sessionId: String, timestamp: Long = System.currentTimeMillis())

    @Transaction
    suspend fun setDefaultSession(serviceId: String, sessionId: String) {
        clearDefaultFlagsForService(serviceId)
        val session = getSessionByIdSync(sessionId)
        if (session != null) {
            updateSession(session.copy(isDefault = true))
        }
    }

    @Query("SELECT COUNT(*) FROM sessions")
    suspend fun countSessions(): Int

    @Query("SELECT COUNT(*) FROM sessions WHERE serviceId = :serviceId")
    suspend fun countSessionsForService(serviceId: String): Int
}
