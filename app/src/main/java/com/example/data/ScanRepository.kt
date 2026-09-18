package com.example.data

import kotlinx.coroutines.flow.Flow

class ScanRepository(private val dao: ScannedRecordDao) {
    val allRecords: Flow<List<ScannedRecord>> = dao.getAllRecords()
    val recordCount: Flow<Int> = dao.getRecordCount()

    fun search(query: String): Flow<List<ScannedRecord>> = dao.searchRecords(query)

    suspend fun getAllList(): List<ScannedRecord> = dao.getAllRecordsList()

    suspend fun insert(content: String, notes: String = ""): Long {
        val record = ScannedRecord(
            content = content,
            notes = notes
        )
        return dao.insertRecord(record)
    }

    suspend fun update(record: ScannedRecord) = dao.updateRecord(record)

    suspend fun delete(record: ScannedRecord) = dao.deleteRecord(record)

    suspend fun deleteById(id: Long) = dao.deleteRecordById(id)

    suspend fun deleteAll() = dao.deleteAllRecords()
}
