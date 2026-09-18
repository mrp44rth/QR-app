package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ScannedRecord
import com.example.export.ExcelExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("QR Scanner", appName)
    }

    @Test
    fun `test format type detection`() {
        assertEquals("URL", ScannedRecord.detectFormatType("https://google.com"))
        assertEquals("URL", ScannedRecord.detectFormatType("http://example.org/test"))
        assertEquals("Wi-Fi", ScannedRecord.detectFormatType("WIFI:T:WPA;S:HomeNetwork;P:password;;"))
        assertEquals("Email", ScannedRecord.detectFormatType("mailto:test@example.com"))
        assertEquals("Phone", ScannedRecord.detectFormatType("tel:+1234567890"))
        assertEquals("Contact", ScannedRecord.detectFormatType("BEGIN:VCARD\nVERSION:3.0\nEND:VCARD"))
        assertEquals("Text", ScannedRecord.detectFormatType("Simple text note"))
    }

    @Test
    fun `test excel export creates valid zip package with spreadsheet entries`() {
        val records = listOf(
            ScannedRecord(
                id = 1,
                content = "https://example.com/product/123",
                notes = "Sample item"
            ),
            ScannedRecord(
                id = 2,
                content = "INVENTORY_BATCH_99",
                notes = "Shelf 4"
            )
        )

        val output = ByteArrayOutputStream()
        ExcelExporter.writeExcelToStream(records, output)
        val bytes = output.toByteArray()
        assertTrue("Excel export should produce non-empty byte array", bytes.isNotEmpty())

        // Verify valid zip structure and required OpenXML files
        val zipIn = ZipInputStream(bytes.inputStream())
        val entries = mutableListOf<String>()
        var entry = zipIn.nextEntry
        while (entry != null) {
            entries.add(entry.name)
            zipIn.closeEntry()
            entry = zipIn.nextEntry
        }

        assertTrue("Contains [Content_Types].xml", entries.contains("[Content_Types].xml"))
        assertTrue("Contains workbook.xml", entries.contains("xl/workbook.xml"))
        assertTrue("Contains sheet1.xml", entries.contains("xl/worksheets/sheet1.xml"))
        assertTrue("Contains styles.xml", entries.contains("xl/styles.xml"))
    }

    @Test
    fun `test csv export output format`() {
        val records = listOf(
            ScannedRecord(
                id = 1,
                content = "https://example.com,with,commas",
                notes = "Note \"quoted\""
            )
        )

        val output = ByteArrayOutputStream()
        ExcelExporter.writeCsvToStream(records, output)
        val csvText = output.toString("UTF-8")

        assertTrue("Contains header", csvText.contains("ID,Scanned Data,Date,Time,Timestamp (ISO),Type,Notes"))
        assertTrue("Properly quotes content with commas", csvText.contains("\"https://example.com,with,commas\""))
    }
}
