package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ScanDatabase
import com.example.data.ScanRepository
import com.example.data.ScannedRecord
import com.example.export.ExcelExporter
import com.example.scanner.QrCodeAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream

enum class ScanAppTab {
    SCANNER,
    RECORDS
}

sealed interface ScanUiEvent {
    data class ShowToast(val message: String) : ScanUiEvent
    data class ExportComplete(val file: File, val isExcel: Boolean) : ScanUiEvent
}

class ScanViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ScanRepository

    init {
        val database = ScanDatabase.getDatabase(application)
        repository = ScanRepository(database.scannedRecordDao())
    }

    private val _currentTab = MutableStateFlow(ScanAppTab.SCANNER)
    val currentTab: StateFlow<ScanAppTab> = _currentTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val records: StateFlow<List<ScannedRecord>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.allRecords
            } else {
                repository.search(query.trim())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val recordCount: StateFlow<Int> = repository.recordCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    private val _recentScan = MutableStateFlow<ScannedRecord?>(null)
    val recentScan: StateFlow<ScannedRecord?> = _recentScan.asStateFlow()

    private val _uiEvents = MutableSharedFlow<ScanUiEvent>()
    val uiEvents: SharedFlow<ScanUiEvent> = _uiEvents.asSharedFlow()

    private val _showExportDialog = MutableStateFlow(false)
    val showExportDialog: StateFlow<Boolean> = _showExportDialog.asStateFlow()

    private val _showManualInputDialog = MutableStateFlow(false)
    val showManualInputDialog: StateFlow<Boolean> = _showManualInputDialog.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    fun setTab(tab: ScanAppTab) {
        _currentTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun dismissRecentScan() {
        _recentScan.value = null
    }

    fun openExportDialog() {
        _showExportDialog.value = true
    }

    fun dismissExportDialog() {
        _showExportDialog.value = false
    }

    fun openManualInputDialog() {
        _showManualInputDialog.value = true
    }

    fun dismissManualInputDialog() {
        _showManualInputDialog.value = false
    }

    fun onQrScanned(rawValue: String) {
        val trimmed = rawValue.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            val record = ScannedRecord(
                content = trimmed,
                notes = ""
            )
            repository.insert(record.content, record.notes)
            _recentScan.value = record
            triggerHapticFeedback()
            _uiEvents.emit(ScanUiEvent.ShowToast("Scanned & saved: ${record.formatType}"))
        }
    }

    fun addManualRecord(content: String, notes: String) {
        if (content.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.insert(content.trim(), notes.trim())
            _uiEvents.emit(ScanUiEvent.ShowToast("Record saved"))
        }
    }

    fun updateRecordNotes(record: ScannedRecord, newNotes: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.update(record.copy(notes = newNotes))
            _uiEvents.emit(ScanUiEvent.ShowToast("Notes updated"))
        }
    }

    fun deleteRecord(record: ScannedRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(record)
            _uiEvents.emit(ScanUiEvent.ShowToast("Record deleted"))
        }
    }

    fun clearAllRecords() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAll()
            _uiEvents.emit(ScanUiEvent.ShowToast("All records cleared"))
        }
    }

    fun scanImageUri(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap != null) {
                    QrCodeAnalyzer.scanBitmap(
                        bitmap = bitmap,
                        onSuccess = { barcodes ->
                            if (barcodes.isNotEmpty()) {
                                for (code in barcodes) {
                                    onQrScanned(code)
                                }
                            } else {
                                viewModelScope.launch {
                                    _uiEvents.emit(ScanUiEvent.ShowToast("No QR code found in selected image"))
                                }
                            }
                        },
                        onFailure = {
                            viewModelScope.launch {
                                _uiEvents.emit(ScanUiEvent.ShowToast("Failed to process image"))
                            }
                        }
                    )
                } else {
                    _uiEvents.emit(ScanUiEvent.ShowToast("Could not open image"))
                }
            } catch (e: Exception) {
                _uiEvents.emit(ScanUiEvent.ShowToast("Error loading image: ${e.localizedMessage}"))
            }
        }
    }

    fun exportToShare(context: Context, isExcel: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            _isExporting.value = true
            try {
                val allList = repository.getAllList()
                if (allList.isEmpty()) {
                    _uiEvents.emit(ScanUiEvent.ShowToast("No records to export"))
                    _isExporting.value = false
                    return@launch
                }
                val file = ExcelExporter.exportToCacheFile(context, allList, isExcel)
                _uiEvents.emit(ScanUiEvent.ExportComplete(file, isExcel))
            } catch (e: Exception) {
                _uiEvents.emit(ScanUiEvent.ShowToast("Export error: ${e.localizedMessage}"))
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun exportToUri(context: Context, uri: Uri, isExcel: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            _isExporting.value = true
            try {
                val allList = repository.getAllList()
                val outputStream: OutputStream? = context.contentResolver.openOutputStream(uri)
                if (outputStream != null) {
                    outputStream.use { os ->
                        if (isExcel) {
                            ExcelExporter.writeExcelToStream(allList, os)
                        } else {
                            ExcelExporter.writeCsvToStream(allList, os)
                        }
                    }
                    _uiEvents.emit(ScanUiEvent.ShowToast("Successfully saved file!"))
                    _showExportDialog.value = false
                } else {
                    _uiEvents.emit(ScanUiEvent.ShowToast("Failed to open storage output"))
                }
            } catch (e: Exception) {
                _uiEvents.emit(ScanUiEvent.ShowToast("Save failed: ${e.localizedMessage}"))
            } finally {
                _isExporting.value = false
            }
        }
    }

    private fun triggerHapticFeedback() {
        try {
            val app = getApplication<Application>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(50)
            }
        } catch (_: Exception) {
            // Silently ignore if vibration permission or hardware unavailable
        }
    }
}
