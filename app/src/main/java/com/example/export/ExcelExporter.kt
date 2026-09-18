package com.example.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.ScannedRecord
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ExcelExporter {

    private fun escapeXml(text: String): String {
        val out = StringBuilder()
        for (char in text) {
            when (char) {
                '&' -> out.append("&amp;")
                '<' -> out.append("&lt;")
                '>' -> out.append("&gt;")
                '"' -> out.append("&quot;")
                '\'' -> out.append("&apos;")
                else -> {
                    // Filter out invalid XML 1.0 control chars except \t, \n, \r
                    if (char.code in 0x20..0xD7FF || char == '\t' || char == '\n' || char == '\r') {
                        out.append(char)
                    }
                }
            }
        }
        return out.toString()
    }

    private fun colName(colIndex: Int): String {
        var n = colIndex
        val sb = StringBuilder()
        while (n >= 0) {
            sb.append(('A'.code + (n % 26)).toChar())
            n = (n / 26) - 1
        }
        return sb.reverse().toString()
    }

    fun writeExcelToStream(records: List<ScannedRecord>, outputStream: OutputStream) {
        val zip = ZipOutputStream(outputStream)

        // 1. [Content_Types].xml
        zip.putNextEntry(ZipEntry("[Content_Types].xml"))
        val contentTypes = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
              <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
              <Default Extension="xml" ContentType="application/xml"/>
              <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
              <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
              <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
            </Types>
        """.trimIndent()
        zip.write(contentTypes.toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()

        // 2. _rels/.rels
        zip.putNextEntry(ZipEntry("_rels/.rels"))
        val rels = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
              <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
            </Relationships>
        """.trimIndent()
        zip.write(rels.toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()

        // 3. xl/_rels/workbook.xml.rels
        zip.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
        val wbRels = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
              <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
              <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
            </Relationships>
        """.trimIndent()
        zip.write(wbRels.toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()

        // 4. xl/workbook.xml
        zip.putNextEntry(ZipEntry("xl/workbook.xml"))
        val workbook = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
              <bookViews>
                <workbookView xWindow="0" yWindow="0" windowWidth="20480" windowHeight="10240"/>
              </bookViews>
              <sheets>
                <sheet name="Scanned QR Data" sheetId="1" r:id="rId1"/>
              </sheets>
            </workbook>
        """.trimIndent()
        zip.write(workbook.toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()

        // 5. xl/styles.xml with header formatting and borders
        zip.putNextEntry(ZipEntry("xl/styles.xml"))
        val styles = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
              <fonts count="2">
                <font>
                  <sz val="11"/>
                  <color theme="1"/>
                  <name val="Segoe UI"/>
                  <family val="2"/>
                </font>
                <font>
                  <b/>
                  <sz val="11"/>
                  <color rgb="FFFFFFFF"/>
                  <name val="Segoe UI"/>
                  <family val="2"/>
                </font>
              </fonts>
              <fills count="3">
                <fill>
                  <patternFill patternType="none"/>
                </fill>
                <fill>
                  <patternFill patternType="gray125"/>
                </fill>
                <fill>
                  <patternFill patternType="solid">
                    <fgColor rgb="FF0F766E"/>
                    <bgColor indexed="64"/>
                  </patternFill>
                </fill>
              </fills>
              <borders count="2">
                <border>
                  <left/><right/><top/><bottom/><diagonal/>
                </border>
                <border>
                  <left style="thin"><color rgb="FFCBD5E1"/></left>
                  <right style="thin"><color rgb="FFCBD5E1"/></right>
                  <top style="thin"><color rgb="FFCBD5E1"/></top>
                  <bottom style="thin"><color rgb="FFCBD5E1"/></bottom>
                </border>
              </borders>
              <cellXfs count="4">
                <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
                <!-- Header: bold white, dark emerald fill, border, center alignment -->
                <xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
                  <alignment horizontal="center" vertical="center" wrapText="0"/>
                </xf>
                <!-- Data: standard with border -->
                <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1" applyAlignment="1">
                  <alignment vertical="center"/>
                </xf>
                <!-- Data Center: center aligned with border -->
                <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1" applyAlignment="1">
                  <alignment horizontal="center" vertical="center"/>
                </xf>
              </cellXfs>
            </styleSheet>
        """.trimIndent()
        zip.write(styles.toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()

        // 6. xl/worksheets/sheet1.xml
        zip.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
        val sheetBuilder = StringBuilder()
        sheetBuilder.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sheetBuilder.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")

        // Column widths
        sheetBuilder.append("""<cols>""")
        sheetBuilder.append("""<col min="1" max="1" width="8" customWidth="1"/>""") // ID
        sheetBuilder.append("""<col min="2" max="2" width="45" customWidth="1"/>""") // Scanned Content
        sheetBuilder.append("""<col min="3" max="3" width="14" customWidth="1"/>""") // Date
        sheetBuilder.append("""<col min="4" max="4" width="14" customWidth="1"/>""") // Time
        sheetBuilder.append("""<col min="5" max="5" width="22" customWidth="1"/>""") // Timestamp ISO
        sheetBuilder.append("""<col min="6" max="6" width="16" customWidth="1"/>""") // Format Type
        sheetBuilder.append("""<col min="7" max="7" width="30" customWidth="1"/>""") // Notes
        sheetBuilder.append("""</cols>""")

        sheetBuilder.append("""<sheetData>""")

        // Row 1: Headers
        val headers = listOf("ID", "Scanned Data", "Date", "Time", "Timestamp (ISO)", "Type", "Notes")
        sheetBuilder.append("""<row r="1" ht="26" customHeight="1">""")
        for ((idx, header) in headers.withIndex()) {
            val cellRef = "${colName(idx)}1"
            sheetBuilder.append("""<c r="$cellRef" s="1" t="inlineStr">""")
            sheetBuilder.append("""<is><t>${escapeXml(header)}</t></is>""")
            sheetBuilder.append("""</c>""")
        }
        sheetBuilder.append("""</row>""")

        // Data Rows
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        for ((index, item) in records.withIndex()) {
            val rowNum = index + 2
            sheetBuilder.append("""<row r="$rowNum" ht="20" customHeight="1">""")

            // Col 0: ID (A)
            sheetBuilder.append("""<c r="A$rowNum" s="3" t="n"><v>${item.id}</v></c>""")

            // Col 1: Scanned Content (B)
            sheetBuilder.append("""<c r="B$rowNum" s="2" t="inlineStr"><is><t>${escapeXml(item.content)}</t></is></c>""")

            // Col 2: Date (C)
            sheetBuilder.append("""<c r="C$rowNum" s="3" t="inlineStr"><is><t>${escapeXml(item.formattedDate)}</t></is></c>""")

            // Col 3: Time (D)
            sheetBuilder.append("""<c r="D$rowNum" s="3" t="inlineStr"><is><t>${escapeXml(item.formattedTime)}</t></is></c>""")

            // Col 4: Timestamp ISO (E)
            val isoString = isoFormat.format(Date(item.timestamp))
            sheetBuilder.append("""<c r="E$rowNum" s="3" t="inlineStr"><is><t>${escapeXml(isoString)}</t></is></c>""")

            // Col 5: Type (F)
            sheetBuilder.append("""<c r="F$rowNum" s="3" t="inlineStr"><is><t>${escapeXml(item.formatType)}</t></is></c>""")

            // Col 6: Notes (G)
            sheetBuilder.append("""<c r="G$rowNum" s="2" t="inlineStr"><is><t>${escapeXml(item.notes)}</t></is></c>""")

            sheetBuilder.append("""</row>""")
        }

        sheetBuilder.append("""</sheetData>""")
        sheetBuilder.append("""</worksheet>""")

        zip.write(sheetBuilder.toString().toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()

        zip.finish()
        outputStream.flush()
    }

    private fun escapeCsv(text: String): String {
        return if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            "\"" + text.replace("\"", "\"\"") + "\""
        } else {
            text
        }
    }

    fun writeCsvToStream(records: List<ScannedRecord>, outputStream: OutputStream) {
        val writer = outputStream.bufferedWriter(StandardCharsets.UTF_8)
        // UTF-8 BOM for Microsoft Excel CSV compatibility
        outputStream.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))

        // Header
        writer.write("ID,Scanned Data,Date,Time,Timestamp (ISO),Type,Notes\n")

        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        for (item in records) {
            val isoString = isoFormat.format(Date(item.timestamp))
            val line = listOf(
                item.id.toString(),
                escapeCsv(item.content),
                escapeCsv(item.formattedDate),
                escapeCsv(item.formattedTime),
                escapeCsv(isoString),
                escapeCsv(item.formatType),
                escapeCsv(item.notes)
            ).joinToString(",")
            writer.write(line + "\n")
        }
        writer.flush()
    }

    fun generateDefaultFileName(extension: String = "xlsx"): String {
        val dateStr = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.getDefault()).format(Date())
        return "QR_Scans_$dateStr.$extension"
    }

    fun exportToCacheFile(context: Context, records: List<ScannedRecord>, isExcel: Boolean = true): File {
        val ext = if (isExcel) "xlsx" else "csv"
        val fileName = generateDefaultFileName(ext)
        val cacheDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(cacheDir, fileName)
        FileOutputStream(file).use { fos ->
            if (isExcel) {
                writeExcelToStream(records, fos)
            } else {
                writeCsvToStream(records, fos)
            }
        }
        return file
    }

    fun createShareIntent(context: Context, file: File, isExcel: Boolean = true): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val mimeType = if (isExcel) {
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        } else {
            "text/csv"
        }
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Exported QR Scans - ${file.name}")
            putExtra(Intent.EXTRA_TEXT, "Here is the exported file containing scanned QR codes with date and time stamps.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
