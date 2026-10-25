package com.example.ui.screens

import android.net.Uri
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.ActiveCallState
import com.example.model.IncomingGestureMode
import com.example.ui.theme.CallGreen
import com.example.ui.theme.CallRed
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomingCallScreen(
  callState: ActiveCallState,
  gestureMode: IncomingGestureMode,
  onAnswer: () -> Unit,
  onDecline: () -> Unit,
  onSendMessage: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  var showQuickMessages by remember { mutableStateOf(false) }
  var dragOffsetX by remember { mutableFloatStateOf(0f) }

  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = 1.12f,
    animationSpec = infiniteRepeatable(
      animation = tween(900),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulseScale"
  )

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF10141D))
      .testTag("incoming_call_screen")
  ) {
    // 1. CALLER BACKGROUND:
    // If contact has a photo -> show FULL SCREEN PHOTO with subtle gradient!
    // If contact does NOT have a photo -> show NO photo at all, only pure dark modern background!
    if (callState.photoUri != null) {
      AsyncImage(
        model = Uri.parse(callState.photoUri),
        contentDescription = "Caller Photo",
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
      )
      // Gradient overlay for readability
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
      // NO PHOTO: Beautiful sleek dark background, no image
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

    // 2. CALLER DETAILS AT TOP
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .statusBarsPadding()
        .padding(top = 70.dp, start = 24.dp, end = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
        color = Color.White.copy(alpha = 0.90f),
        fontWeight = FontWeight.Normal,
        textAlign = TextAlign.Center
      )
    }

    // 3. BOTTOM CONTROLS
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.BottomCenter)
        .navigationBarsPadding()
        .padding(horizontal = 24.dp, vertical = 28.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // "Message" button (pill)
      Surface(
        onClick = { showQuickMessages = true },
        shape = CircleShape,
        color = Color.White,
        shadowElevation = 4.dp,
        modifier = Modifier
          .padding(bottom = 28.dp)
          .testTag("incoming_message_button")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.ChatBubbleOutline,
            contentDescription = "Quick message",
            tint = Color(0xFF1F1F1F),
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = "Message",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1F1F1F)
          )
        }
      }

      // GESTURE ANSWER / DECLINE CONTAINER
      Surface(
        shape = RoundedCornerShape(36.dp),
        color = Color(0xFFE8EEF4),
        modifier = Modifier
          .fillMaxWidth()
          .height(86.dp)
          .testTag("incoming_gesture_card")
      ) {
        if (gestureMode == IncomingGestureMode.HORIZONTAL_SWIPE) {
          // Horizontal swipe mode (draggable center phone icon)
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
          ) {
            // "Decline" label on left
            Text(
              text = "Decline",
              fontSize = 18.sp,
              fontWeight = FontWeight.Medium,
              color = Color(0xFF1F1F1F),
              modifier = Modifier
                .align(Alignment.CenterStart)
                .clickable { onDecline() }
                .testTag("gesture_decline_label")
            )

            // "Answer" label on right
            Text(
              text = "Answer",
              fontSize = 18.sp,
              fontWeight = FontWeight.Medium,
              color = Color(0xFF1F1F1F),
              modifier = Modifier
                .align(Alignment.CenterEnd)
                .clickable { onAnswer() }
                .testTag("gesture_answer_label")
            )

            // Center draggable call icon
            Box(
              modifier = Modifier
                .offset { IntOffset(dragOffsetX.roundToInt(), 0) }
                .draggable(
                  state = rememberDraggableState { delta ->
                    dragOffsetX = (dragOffsetX + delta).coerceIn(-240f, 240f)
                    if (dragOffsetX > 150f) {
                      onAnswer()
                    } else if (dragOffsetX < -150f) {
                      onDecline()
                    }
                  },
                  orientation = Orientation.Horizontal,
                  onDragStopped = {
                    dragOffsetX = 0f
                  }
                )
                .size(68.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(Color.White)
                .clickable { onAnswer() }
                .testTag("draggable_call_handle"),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Call,
                contentDescription = "Swipe right to answer, left to decline",
                tint = CallGreen,
                modifier = Modifier.size(34.dp)
              )
            }
          }
        } else {
          // Single tap mode: Two prominent buttons
          Row(
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Surface(
              onClick = onDecline,
              shape = CircleShape,
              color = CallRed,
              modifier = Modifier
                .size(64.dp)
                .testTag("tap_decline_button")
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.CallEnd,
                  contentDescription = "Decline",
                  tint = Color.White,
                  modifier = Modifier.size(30.dp)
                )
              }
            }

            Text(
              text = "Incoming call",
              fontSize = 15.sp,
              color = Color(0xFF444746)
            )

            Surface(
              onClick = onAnswer,
              shape = CircleShape,
              color = CallGreen,
              modifier = Modifier
                .size(64.dp)
                .testTag("tap_answer_button")
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.Call,
                  contentDescription = "Answer",
                  tint = Color.White,
                  modifier = Modifier.size(30.dp)
                )
              }
            }
          }
        }
      }
    }

    // Quick Responses Bottom Sheet
    if (showQuickMessages) {
      ModalBottomSheet(
        onDismissRequest = { showQuickMessages = false },
        sheetState = rememberModalBottomSheetState(),
        containerColor = Color.White
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
          Text(
            text = "Quick responses",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1F1F1F)
          )

          Spacer(modifier = Modifier.height(16.dp))

          val canned = listOf(
            "Can't talk now. What's up?",
            "I'll call you right back.",
            "I'm on my way.",
            "Can you call me later?"
          )

          canned.forEach { msg ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  showQuickMessages = false
                  onSendMessage(msg)
                }
                .padding(vertical = 14.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = msg,
                fontSize = 16.sp,
                color = Color(0xFF1F1F1F)
              )
            }
          }

          Spacer(modifier = Modifier.height(24.dp))
        }
      }
    }
  }
}
