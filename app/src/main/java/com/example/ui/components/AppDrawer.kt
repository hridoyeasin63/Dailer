package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.NoPhotography
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PhoneCallback
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.Screen
import com.example.ui.theme.CallGreen
import com.example.ui.theme.PhoneBlue

@Composable
fun AppDrawerSheet(
  onNavigate: (Screen) -> Unit,
  onClearHistory: () -> Unit,
  onSimulateIncomingCallWithPhoto: () -> Unit,
  onSimulateIncomingCallNoPhoto: () -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier
      .width(300.dp)
      .fillMaxHeight(),
    color = Color.White,
    shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .statusBarsPadding()
        .padding(vertical = 24.dp)
    ) {
      Text(
        text = "Phone",
        fontSize = 24.sp,
        fontWeight = FontWeight.Normal,
        color = Color(0xFF1F1F1F),
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
      )

      Spacer(modifier = Modifier.height(8.dp))

      DrawerItem(
        icon = Icons.Default.People,
        label = "Contacts",
        onClick = {
          onClose()
          onNavigate(Screen.CONTACTS)
        },
        testTag = "drawer_item_contacts"
      )

      DrawerItem(
        icon = Icons.Default.Settings,
        label = "Settings",
        onClick = {
          onClose()
          onNavigate(Screen.SETTINGS)
        },
        testTag = "drawer_item_settings"
      )

      DrawerItem(
        icon = Icons.Default.History,
        label = "Clear call history",
        onClick = {
          onClose()
          onClearHistory()
        },
        testTag = "drawer_item_clear_history"
      )

      DrawerItem(
        icon = Icons.Default.HelpOutline,
        label = "Help & feedback",
        onClick = {
          onClose()
        },
        testTag = "drawer_item_help"
      )

      Spacer(modifier = Modifier.height(16.dp))
      HorizontalDivider(
        modifier = Modifier.padding(horizontal = 24.dp),
        color = Color(0xFFE8EEF4)
      )
      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = "TEST CALL SCREEN UI",
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF747775),
        letterSpacing = 1.sp,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
      )

      // Test incoming call with photo
      DrawerItem(
        icon = Icons.Default.Image,
        label = "Test Call (With Photo)",
        iconTint = CallGreen,
        onClick = {
          onClose()
          onSimulateIncomingCallWithPhoto()
        },
        testTag = "drawer_item_test_with_photo"
      )

      // Test incoming call without photo
      DrawerItem(
        icon = Icons.Default.NoPhotography,
        label = "Test Call (No Photo)",
        iconTint = PhoneBlue,
        onClick = {
          onClose()
          onSimulateIncomingCallNoPhoto()
        },
        testTag = "drawer_item_test_no_photo"
      )
    }
  }
}

@Composable
private fun DrawerItem(
  icon: ImageVector,
  label: String,
  onClick: () -> Unit,
  testTag: String,
  iconTint: Color = Color(0xFF444746)
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .padding(horizontal = 24.dp, vertical = 14.dp)
      .testTag(testTag),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      imageVector = icon,
      contentDescription = label,
      tint = iconTint,
      modifier = Modifier.size(24.dp)
    )
    Spacer(modifier = Modifier.width(20.dp))
    Text(
      text = label,
      fontSize = 15.sp,
      color = Color(0xFF1F1F1F),
      fontWeight = FontWeight.Medium
    )
  }
}
