package com.example.ui.screens

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.ActiveCallState
import com.example.model.AudioOutputMode
import com.example.ui.theme.CallRed
import com.example.ui.theme.PhoneBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveCallScreen(
  callState: ActiveCallState,
  showInCallKeypad: Boolean,
  onToggleKeypad: () -> Unit,
  onToggleMute: () -> Unit,
  onCycleAudio: () -> Unit,
  onToggleRecord: () -> Unit,
  onToggleHold: () -> Unit,
  onEndCall: () -> Unit,
  onKeypadDigit: (Char) -> Unit,
  modifier: Modifier = Modifier
) {
  var showMoreMenu by remember { mutableStateOf(false) }

  val formattedDuration = remember(callState.durationSeconds) {
    val mins = callState.durationSeconds / 60
    val secs = callState.durationSeconds % 60
    String.format("%02d:%02d", mins, secs)
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF10141D))
      .testTag("active_call_screen")
  ) {
    // Background:
    // If contact has photo -> show FULL SCREEN PHOTO with subtle gradient!
    // If contact does NOT have photo -> show NO photo at all, clean dark background!
    if (callState.photoUri != null) {
      AsyncImage(
        model = Uri.parse(callState.photoUri),
        contentDescription = "Caller Photo",
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
      )
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.verticalGradient(
              colors = listOf(
                Color.Black.copy(alpha = 0.55f),
                Color.Transparent,
                Color.Black.copy(alpha = 0.70f)
              )
            )
          )
      )
    } else if (callState.avatarResId != null) {
      Image(
        painter = painterResource(id = callState.avatarResId),
        contentDescription = "Caller Photo",
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
      )
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.verticalGradient(
              colors = listOf(
                Color.Black.copy(alpha = 0.55f),
                Color.Transparent,
                Color.Black.copy(alpha = 0.70f)
              )
            )
          )
      )
    } else {
      // NO PHOTO: Clean dark gradient, no image shown
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.verticalGradient(
              colors = listOf(
                Color(0xFF1E232F),
                Color(0xFF12151D),
                Color(0xFF0C0E14)
              )
            )
          )
      )
    }

    // Top caller information
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .statusBarsPadding()
        .padding(top = 40.dp, start = 24.dp, end = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      val statusText = if (!callState.isConnected) {
        "Calling via ${callState.carrier}"
      } else {
        "Call connected • $formattedDuration"
      }

      Text(
        text = statusText,
        fontSize = 17.sp,
        fontWeight = FontWeight.Medium,
        color = Color.White.copy(alpha = 0.9f),
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(14.dp))

      Text(
        text = callState.callerName,
        fontSize = 38.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = "Mobile ${callState.phoneNumber}",
        fontSize = 18.sp,
        color = Color.White.copy(alpha = 0.95f),
        textAlign = TextAlign.Center
      )

      if (callState.isRecording) {
        Spacer(modifier = Modifier.height(12.dp))
        Surface(
          shape = CircleShape,
          color = Color(0x99D93025)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Color.White)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Recording",
              color = Color.White,
              fontSize = 13.sp,
              fontWeight = FontWeight.Medium
            )
          }
        }
      }
    }

    // Bottom in-call control card
    Surface(
      shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
      color = Color(0xFFF1F4F9),
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.BottomCenter)
        .navigationBarsPadding()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Controls Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceAround
        ) {
          // 1. Keypad
          InCallControlButton(
            icon = Icons.Default.Dialpad,
            label = "Keypad",
            isActive = showInCallKeypad,
            onClick = onToggleKeypad,
            testTag = "incall_keypad_button"
          )

          // 2. Mute
          InCallControlButton(
            icon = if (callState.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
            label = "Mute",
            isActive = callState.isMuted,
            onClick = onToggleMute,
            testTag = "incall_mute_button"
          )

          // 3. Audio output (Bluetooth / Speaker / Phone)
          val audioIcon = when (callState.audioOutput) {
            AudioOutputMode.BLUETOOTH -> Icons.Default.BluetoothAudio
            AudioOutputMode.SPEAKER -> Icons.Default.VolumeUp
            AudioOutputMode.EARPIECE -> Icons.Default.PhoneInTalk
          }
          InCallControlButton(
            icon = audioIcon,
            label = callState.audioOutput.label,
            isActive = callState.audioOutput != AudioOutputMode.EARPIECE,
            onClick = onCycleAudio,
            testTag = "incall_audio_button"
          )

          // 4. More
          Box {
            InCallControlButton(
              icon = Icons.Default.MoreVert,
              label = "More",
              isActive = false,
              onClick = { showMoreMenu = true },
              testTag = "incall_more_button"
            )

            DropdownMenu(
              expanded = showMoreMenu,
              onDismissRequest = { showMoreMenu = false }
            ) {
              DropdownMenuItem(
                text = { Text(if (callState.isRecording) "Stop recording" else "Record call") },
                onClick = {
                  showMoreMenu = false
                  onToggleRecord()
                }
              )
              DropdownMenuItem(
                text = { Text(if (callState.isHold) "Unhold" else "Hold call") },
                onClick = {
                  showMoreMenu = false
                  onToggleHold()
                }
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Red Hang Up Button
        Surface(
          onClick = onEndCall,
          shape = CircleShape,
          color = CallRed,
          shadowElevation = 4.dp,
          modifier = Modifier
            .width(130.dp)
            .height(64.dp)
            .testTag("incall_end_call_button")
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.CallEnd,
              contentDescription = "End Call",
              tint = Color.White,
              modifier = Modifier.size(32.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
      }
    }

    // In-call keypad popup dialog
    if (showInCallKeypad) {
      BasicAlertDialog(
        onDismissRequest = onToggleKeypad,
        modifier = Modifier.testTag("incall_keypad_dialog")
      ) {
        Surface(
          shape = RoundedCornerShape(24.dp),
          color = Color.White
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = "Keypad",
              fontSize = 20.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF1F1F1F)
            )
            Spacer(modifier = Modifier.height(16.dp))

            val keys = listOf(
              listOf('1', '2', '3'),
              listOf('4', '5', '6'),
              listOf('7', '8', '9'),
              listOf('*', '0', '#')
            )

            keys.forEach { row ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
              ) {
                row.forEach { k ->
                  Surface(
                    onClick = { onKeypadDigit(k) },
                    shape = CircleShape,
                    color = Color(0xFFF1F4F9),
                    modifier = Modifier.size(60.dp)
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Text(
                        text = k.toString(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1F1F1F)
                      )
                    }
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(14.dp))
            TextButton(onClick = onToggleKeypad) {
              Text("Close", color = PhoneBlue, fontSize = 16.sp)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun InCallControlButton(
  icon: ImageVector,
  label: String,
  isActive: Boolean,
  onClick: () -> Unit,
  testTag: String
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.testTag(testTag)
  ) {
    Surface(
      onClick = onClick,
      shape = CircleShape,
      color = if (isActive) Color(0xFF0B2447) else Color.White,
      shadowElevation = 1.dp,
      modifier = Modifier.size(64.dp)
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = icon,
          contentDescription = label,
          tint = if (isActive) Color.White else Color(0xFF1F1F1F),
          modifier = Modifier.size(28.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = label,
      fontSize = 13.sp,
      fontWeight = FontWeight.Medium,
      color = Color(0xFF1F1F1F),
      textAlign = TextAlign.Center
    )
  }
}
