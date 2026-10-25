package com.example.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.IncomingGestureMode
import com.example.ui.theme.PhoneBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomingGestureDialog(
  currentMode: IncomingGestureMode,
  onSelectMode: (IncomingGestureMode) -> Unit,
  onDismiss: () -> Unit
) {
  BasicAlertDialog(
    onDismissRequest = onDismiss,
    modifier = Modifier.testTag("incoming_gesture_dialog")
  ) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = Color.White,
      shadowElevation = 6.dp
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp)
      ) {
        Text(
          text = "Incoming call gesture",
          fontSize = 20.sp,
          fontWeight = FontWeight.Medium,
          color = Color(0xFF1F1F1F)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Option 1: Horizontal swipe
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectMode(IncomingGestureMode.HORIZONTAL_SWIPE) }
            .padding(vertical = 12.dp)
            .testTag("gesture_option_horizontal_swipe"),
          verticalAlignment = Alignment.CenterVertically
        ) {
          RadioButton(
            selected = currentMode == IncomingGestureMode.HORIZONTAL_SWIPE,
            onClick = { onSelectMode(IncomingGestureMode.HORIZONTAL_SWIPE) },
            colors = RadioButtonDefaults.colors(selectedColor = PhoneBlue)
          )
          Spacer(modifier = Modifier.width(12.dp))
          Text(
            text = "Horizontal swipe",
            fontSize = 17.sp,
            color = Color(0xFF1F1F1F)
          )
        }

        // Option 2: Single tap
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectMode(IncomingGestureMode.SINGLE_TAP) }
            .padding(vertical = 12.dp)
            .testTag("gesture_option_single_tap"),
          verticalAlignment = Alignment.CenterVertically
        ) {
          RadioButton(
            selected = currentMode == IncomingGestureMode.SINGLE_TAP,
            onClick = { onSelectMode(IncomingGestureMode.SINGLE_TAP) },
            colors = RadioButtonDefaults.colors(selectedColor = PhoneBlue)
          )
          Spacer(modifier = Modifier.width(12.dp))
          Text(
            text = "Single tap",
            fontSize = 17.sp,
            color = Color(0xFF1F1F1F)
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Spacer(modifier = Modifier.weight(1f))
          TextButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("gesture_dialog_cancel")
          ) {
            Text(
              text = "Cancel",
              color = PhoneBlue,
              fontSize = 15.sp,
              fontWeight = FontWeight.Medium
            )
          }
        }
      }
    }
  }
}
