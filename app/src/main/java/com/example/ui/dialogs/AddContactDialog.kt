package com.example.ui.dialogs

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.PhoneBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddContactDialog(
  initialNumber: String? = null,
  onSave: (name: String, number: String, label: String, photoUri: String?) -> Unit,
  onDismiss: () -> Unit
) {
  var name by remember { mutableStateOf("") }
  var number by remember { mutableStateOf(initialNumber ?: "") }
  var label by remember { mutableStateOf("Mobile") }
  var photoUri by remember { mutableStateOf<String?>(null) }

  // Standard Android Photo Picker for picking contact picture
  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    photoUri = uri?.toString()
  }

  BasicAlertDialog(
    onDismissRequest = onDismiss,
    modifier = Modifier.testTag("add_contact_dialog")
  ) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = Color.White,
      shadowElevation = 6.dp
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = "Create new contact",
          fontSize = 20.sp,
          fontWeight = FontWeight.Medium,
          color = Color(0xFF1F1F1F),
          modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Contact Photo picker avatar circle
        Box(contentAlignment = Alignment.BottomEnd) {
          Surface(
            modifier = Modifier
              .size(80.dp)
              .clip(CircleShape)
              .clickable {
                photoPickerLauncher.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              }
              .testTag("pick_contact_photo_button"),
            color = Color(0xFFEFF3F8),
            shape = CircleShape
          ) {
            if (photoUri != null) {
              AsyncImage(
                model = Uri.parse(photoUri),
                contentDescription = "Selected contact photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth()
              )
            } else {
              Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.AddAPhoto,
                  contentDescription = "Add photo",
                  tint = PhoneBlue,
                  modifier = Modifier.size(32.dp)
                )
              }
            }
          }

          if (photoUri != null) {
            Box(
              modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(Color(0xFF5F6368))
                .clickable { photoUri = null },
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove photo",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
              )
            }
          }
        }

        Text(
          text = if (photoUri != null) "Photo selected" else "Tap to add contact photo (optional)",
          fontSize = 12.sp,
          color = Color(0xFF747775),
          modifier = Modifier.padding(top = 6.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Name") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("contact_name_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = number,
          onValueChange = { number = it },
          label = { Text("Phone number") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("contact_number_input")
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Spacer(modifier = Modifier.weight(1f))
          TextButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("add_contact_cancel")
          ) {
            Text("Cancel", color = Color(0xFF444746))
          }
          Button(
            onClick = {
              if (name.isNotBlank() && number.isNotBlank()) {
                onSave(name.trim(), number.trim(), label, photoUri)
              }
            },
            enabled = name.isNotBlank() && number.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = PhoneBlue),
            modifier = Modifier.testTag("add_contact_save")
          ) {
            Text("Save")
          }
        }
      }
    }
  }
}
