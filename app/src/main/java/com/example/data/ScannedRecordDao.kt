package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScannedRecordDao {
    @Query("SELECT * FROM scanned_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<ScannedRecord>>

    @Query("SELECT * FROM scanned_records WHERE content LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchRecords(query: String): Flow<List<ScannedRecord>>

    @Query("SELECT * FROM scanned_records ORDER BY timestamp DESC")
    suspend fun getAllRecordsList(): List<ScannedRecord>

    @Query("SELECT COUNT(*) FROM scanned_records")
    fun getRecordCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: ScannedRecord): Long

    @Update
    suspend fun updateRecord(record: ScannedRecord)

    @Delete
    suspend fun deleteRecord(record: ScannedRecord)

    @Query("DELETE FROM scanned_records WHERE id = :id")
    suspend fun deleteRecordById(id: Long)

    @Query("DELETE FROM scanned_records")
    suspend fun deleteAllRecords()
}
