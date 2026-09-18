package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ManualInputDialog(
    onDismiss: () -> Unit,
    onSubmit: (content: String, notes: String) -> Unit
) {
    var content by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val sampleQrs = listOf(
        "https://ai.google.dev/studio",
        "WIFI:T:WPA;S:OfficeGuest_5G;P:SecurePass2026;;",
        "PRODUCT_SKU_9823471029",
        "BEGIN:VCARD\nVERSION:3.0\nN:Arya;Vikash\nTEL:+1555123456\nEMAIL:vikash@example.com\nEND:VCARD",
        "INVENTORY_ITEM_BOX_B42_LOC9"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Scanned Data",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Enter QR code content or select a sample for quick testing.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Scanned Content *") },
                    placeholder = { Text("e.g. https://example.com or SKU#") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_content_input"),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2,
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Optional Notes") },
                    placeholder = { Text("e.g. Warehouse rack 3, or Event attendee") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_notes_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Quick test sample button
                OutlinedButton(
                    onClick = {
                        val sample = sampleQrs.random()
                        content = sample
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Insert Sample Test QR Data", fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (content.isNotBlank()) {
                        onSubmit(content, notes)
                        onDismiss()
                    }
                },
                enabled = content.isNotBlank(),
                modifier = Modifier.testTag("submit_manual_entry_button")
            ) {
                Text("Save Record")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
