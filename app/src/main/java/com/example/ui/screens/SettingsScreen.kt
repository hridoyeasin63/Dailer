package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Voicemail
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.model.IncomingGestureMode
import com.example.ui.theme.BackgroundLight
import com.example.ui.theme.CallGreen
import com.example.ui.theme.PhoneBlue
import com.example.ui.theme.PhoneBlueText

@Composable
fun SettingsScreen(
  gestureMode: IncomingGestureMode,
  isDefaultDialer: Boolean,
  onRequestDefaultDialer: () -> Unit,
  onOpenGestureDialog: () -> Unit,
  onBack: () -> Unit,
  onOpenBlockedNumbers: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .background(BackgroundLight)
      .statusBarsPadding()
      .testTag("settings_screen")
  ) {
    // Top app bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(60.dp)
        .padding(horizontal = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(
        onClick = onBack,
        modifier = Modifier.testTag("settings_back_button")
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = "Back",
          tint = Color(0xFF1F1F1F)
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      Text(
        text = "Settings",
        fontSize = 22.sp,
        fontWeight = FontWeight.Normal,
        color = Color(0xFF1F1F1F)
      )
    }

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
      // 0. Default Phone App card
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
          containerColor = if (isDefaultDialer) Color(0xFFE6F4EA) else Color.White
        ),
        elevation = CardDefaults.cardElevation(0.5.dp)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onRequestDefaultDialer() }
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag("default_dialer_setting_card"),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = if (isDefaultDialer) Icons.Default.CheckCircle else Icons.Default.PhoneAndroid,
            contentDescription = null,
            tint = if (isDefaultDialer) CallGreen else PhoneBlue,
            modifier = Modifier.size(28.dp)
          )
          Spacer(modifier = Modifier.width(16.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = if (isDefaultDialer) "Default Phone App" else "Set as default phone app",
              fontSize = 16.sp,
              fontWeight = FontWeight.Medium,
              color = Color(0xFF1F1F1F)
            )
            Text(
              text = if (isDefaultDialer) "Active • Handling incoming & outgoing calls" else "Tap to set as your default phone dialer",
              fontSize = 13.sp,
              color = Color(0xFF5F6368),
              modifier = Modifier.padding(top = 2.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      // 1. Call Assist section
      Text(
        text = "Call Assist",
        color = PhoneBlueText,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
      )

      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.5.dp)
      ) {
        SettingsItemRow(
          icon = Icons.Default.Info,
          title = "Caller ID & spam",
          onClick = { }
        )
      }

      Spacer(modifier = Modifier.height(18.dp))

      // 2. General section
      Text(
        text = "General",
        color = PhoneBlueText,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
      )

      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.5.dp)
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          SettingsItemRow(
            icon = Icons.Default.Accessibility,
            title = "Accessibility",
            onClick = { }
          )
          ItemDivider()

          SettingsItemRow(
            icon = Icons.Default.Language,
            title = "Assisted dialing",
            onClick = { }
          )
          ItemDivider()

          SettingsItemRow(
            icon = Icons.Default.Block,
            title = "Blocked numbers",
            onClick = onOpenBlockedNumbers
          )
          ItemDivider()

          SettingsItemRow(
            icon = Icons.Default.SimCard,
            title = "Calling accounts",
            onClick = { }
          )
          ItemDivider()

          SettingsItemRow(
            icon = Icons.Default.RadioButtonChecked,
            title = "Call recording",
            onClick = { }
          )
          ItemDivider()

          SettingsItemRow(
            icon = Icons.Default.FormatListBulleted,
            title = "Display options",
            onClick = { }
          )
          ItemDivider()

          // INCOMING CALL GESTURE
          SettingsItemRow(
            icon = Icons.Default.Gesture,
            title = "Incoming call gesture",
            subtitle = gestureMode.displayName,
            onClick = onOpenGestureDialog
          )
          ItemDivider()

          SettingsItemRow(
            icon = Icons.Default.Message,
            title = "Quick responses",
            onClick = { }
          )
          ItemDivider()

          SettingsItemRow(
            icon = Icons.Default.VolumeUp,
            title = "Sounds and vibration",
            onClick = { }
          )
          ItemDivider()

          SettingsItemRow(
            icon = Icons.Default.Voicemail,
            title = "Voicemail",
            onClick = { }
          )
          ItemDivider()

          SettingsItemRow(
            icon = Icons.Default.MusicNote,
            title = "Contact ringtones",
            onClick = { }
          )
          ItemDivider()

          SettingsItemRow(
            icon = Icons.Default.CreditCard,
            title = "Calling card",
            onClick = { }
          )
        }
      }

      Spacer(modifier = Modifier.height(32.dp))
    }
  }
}

@Composable
private fun SettingsItemRow(
  icon: ImageVector,
  title: String,
  subtitle: String? = null,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .padding(horizontal = 20.dp, vertical = 16.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = Color(0xFF444746),
      modifier = Modifier.size(24.dp)
    )
    Spacer(modifier = Modifier.width(20.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        color = Color(0xFF1F1F1F)
      )
      if (subtitle != null) {
        Text(
          text = subtitle,
          fontSize = 13.sp,
          color = Color(0xFF747775),
          modifier = Modifier.padding(top = 2.dp)
        )
      }
    }
  }
}

@Composable
private fun ItemDivider() {
  HorizontalDivider(
    modifier = Modifier.padding(start = 64.dp),
    color = Color(0xFFF1F4F9),
    thickness = 1.dp
  )
}
