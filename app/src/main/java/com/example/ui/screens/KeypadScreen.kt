package com.example.ui.screens

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Voicemail
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.Contact
import com.example.ui.theme.CallGreen
import com.example.ui.theme.DialpadBg
import com.example.ui.theme.DialpadKeyBg
import com.example.ui.theme.DialpadKeyBorder
import com.example.ui.theme.PhoneBlueText
import com.example.util.T9MatchResult

data class KeypadButtonData(
  val digit: Char,
  val letters: String,
  val hasVoicemailIcon: Boolean = false
)

private val dialpadGrid = listOf(
  listOf(
    KeypadButtonData('1', "", hasVoicemailIcon = true),
    KeypadButtonData('2', "ABC"),
    KeypadButtonData('3', "DEF")
  ),
  listOf(
    KeypadButtonData('4', "GHI"),
    KeypadButtonData('5', "JKL"),
    KeypadButtonData('6', "MNO")
  ),
  listOf(
    KeypadButtonData('7', "PQRS"),
    KeypadButtonData('8', "TUV"),
    KeypadButtonData('9', "WXYZ")
  ),
  listOf(
    KeypadButtonData('*', ""),
    KeypadButtonData('0', "+"),
    KeypadButtonData('#', "")
  )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KeypadScreen(
  keypadInput: String,
  t9Matches: List<T9MatchResult>,
  onDigitPress: (Char) -> Unit,
  onBackspace: () -> Unit,
  onClear: () -> Unit,
  onCall: (name: String, number: String, avatar: Int?, photoUri: String?) -> Unit,
  onAddContact: (number: String) -> Unit,
  modifier: Modifier = Modifier
) {
  var showMenu by remember { mutableStateOf(false) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFFF1F5F9))
      .statusBarsPadding()
      .testTag("keypad_screen")
  ) {
    // 1. TOP SECTION: Suggested or All contacts list matching T9 input
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
        .padding(horizontal = 16.dp)
    ) {
      Text(
        text = if (keypadInput.isEmpty()) "Suggested" else "All contacts",
        color = PhoneBlueText,
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
      )

      Card(
        modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.5.dp)
      ) {
        if (t9Matches.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(100.dp)
              .padding(16.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = if (keypadInput.isEmpty()) "No contacts yet" else "No matching contact",
              color = Color(0xFF747775),
              fontSize = 14.sp
            )
          }
        } else {
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .testTag("keypad_suggestions_list")
          ) {
            items(t9Matches.take(6), key = { it.contact.id }) { match ->
              KeypadContactRow(
                match = match,
                onCall = {
                  onCall(
                    match.contact.name,
                    match.contact.phoneNumber,
                    match.contact.avatarResId,
                    match.contact.photoUri
                  )
                }
              )
            }
          }
        }
      }
    }

    // 2. DIALED NUMBER DISPLAY ROW
    Surface(
      modifier = Modifier.fillMaxWidth(),
      color = DialpadBg,
      shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 10.dp, bottom = 6.dp)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Left: 3-dots menu
          Box {
            IconButton(
              onClick = { showMenu = true },
              modifier = Modifier.testTag("dialpad_options_menu")
            ) {
              Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Options",
                tint = Color(0xFF444746)
              )
            }
            DropdownMenu(
              expanded = showMenu,
              onDismissRequest = { showMenu = false }
            ) {
              DropdownMenuItem(
                text = { Text("Add to contact") },
                onClick = {
                  showMenu = false
                  onAddContact(keypadInput)
                }
              )
              DropdownMenuItem(
                text = { Text("Clear all") },
                onClick = {
                  showMenu = false
                  onClear()
                }
              )
            }
          }

          // Center: Large dialed digits
          Text(
            text = keypadInput,
            fontSize = 34.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF1F1F1F),
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier
              .weight(1f)
              .testTag("dialed_number_display")
          )

          // Right: Backspace button
          if (keypadInput.isNotEmpty()) {
            Surface(
              modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .combinedClickable(
                  onClick = onBackspace,
                  onLongClick = onClear
                )
                .testTag("dialpad_backspace_button"),
              color = Color.Transparent
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.Backspace,
                  contentDescription = "Backspace",
                  tint = Color(0xFF444746),
                  modifier = Modifier.size(24.dp)
                )
              }
            }
          } else {
            Spacer(modifier = Modifier.size(46.dp))
          }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 3. 3x4 KEYPAD GRID
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          dialpadGrid.forEach { row ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              row.forEach { item ->
                DialpadKey(
                  data = item,
                  onPress = { onDigitPress(item.digit) },
                  onLongPress = {
                    if (item.digit == '0') {
                      onDigitPress('+')
                    } else if (item.digit == '1') {
                      // Voicemail
                      onCall("Voicemail", "121", null, null)
                    }
                  }
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // 4. BIG GREEN CALL BUTTON
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
          ) {
            Button(
              onClick = {
                val targetNumber = if (keypadInput.isNotBlank()) keypadInput else ""
                if (targetNumber.isNotEmpty()) {
                  onCall(targetNumber, targetNumber, null, null)
                }
              },
              shape = CircleShape,
              colors = ButtonDefaults.buttonColors(containerColor = CallGreen),
              modifier = Modifier
                .width(160.dp)
                .height(58.dp)
                .testTag("call_action_fab")
            ) {
              Icon(
                imageVector = Icons.Default.Call,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = "Call",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))
        }
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DialpadKey(
  data: KeypadButtonData,
  onPress: () -> Unit,
  onLongPress: () -> Unit
) {
  Surface(
    modifier = Modifier
      .width(96.dp)
      .height(64.dp)
      .clip(CircleShape)
      .border(1.dp, DialpadKeyBorder, CircleShape)
      .combinedClickable(
        onClick = onPress,
        onLongClick = onLongPress
      )
      .testTag("dialpad_key_${data.digit}"),
    shape = CircleShape,
    color = DialpadKeyBg
  ) {
    Column(
      modifier = Modifier.fillMaxSize(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Text(
        text = data.digit.toString(),
        fontSize = 24.sp,
        fontWeight = FontWeight.Medium,
        color = Color(0xFF1F1F1F)
      )
      if (data.hasVoicemailIcon) {
        Icon(
          imageVector = Icons.Default.Voicemail,
          contentDescription = "Voicemail",
          tint = Color(0xFF747775),
          modifier = Modifier.size(12.dp)
        )
      } else if (data.letters.isNotEmpty()) {
        Text(
          text = data.letters,
          fontSize = 11.sp,
          fontWeight = FontWeight.Normal,
          color = Color(0xFF747775),
          letterSpacing = 1.sp
        )
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun KeypadContactRow(
  match: T9MatchResult,
  onCall: () -> Unit
) {
  val contact = match.contact
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .combinedClickable(onClick = onCall)
      .padding(horizontal = 16.dp, vertical = 12.dp)
      .testTag("keypad_contact_${contact.id}"),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Avatar
    if (contact.photoUri != null) {
      AsyncImage(
        model = Uri.parse(contact.photoUri),
        contentDescription = contact.name,
        contentScale = ContentScale.Crop,
        modifier = Modifier
          .size(46.dp)
          .clip(CircleShape)
      )
    } else if (contact.avatarResId != null) {
      Image(
        painter = painterResource(id = contact.avatarResId),
        contentDescription = contact.name,
        contentScale = ContentScale.Crop,
        modifier = Modifier
          .size(46.dp)
          .clip(CircleShape)
      )
    } else {
      Box(
        modifier = Modifier
          .size(46.dp)
          .clip(CircleShape)
          .background(Color(0xFFE7ECF3)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Person,
          contentDescription = null,
          tint = Color(0xFF757B82),
          modifier = Modifier.size(26.dp)
        )
      }
    }

    Spacer(modifier = Modifier.width(14.dp))

    // Name & Number with T9 Highlight
    Column(modifier = Modifier.weight(1f)) {
      val annotatedName = buildAnnotatedString {
        val name = contact.name
        val ranges = match.nameHighlightRanges
        for (i in name.indices) {
          val isHighlighted = ranges.any { i in it }
          if (isHighlighted) {
            withStyle(
              style = SpanStyle(
                color = Color(0xFF0B57D0),
                fontWeight = FontWeight.Bold
              )
            ) {
              append(name[i])
            }
          } else {
            withStyle(
              style = SpanStyle(
                color = Color(0xFF1F1F1F),
                fontWeight = FontWeight.Normal
              )
            ) {
              append(name[i])
            }
          }
        }
      }

      Text(
        text = annotatedName,
        fontSize = 16.sp
      )

      val annotatedNumber = buildAnnotatedString {
        append("${contact.label} ")
        val num = contact.phoneNumber
        val range = match.numberHighlightRange
        if (range != null) {
          for (i in num.indices) {
            if (i in range) {
              withStyle(
                style = SpanStyle(
                  color = Color(0xFF0B57D0),
                  fontWeight = FontWeight.Bold
                )
              ) {
                append(num[i])
              }
            } else {
              withStyle(
                style = SpanStyle(
                  color = Color(0xFF5F6368),
                  fontWeight = FontWeight.Normal
                )
              ) {
                append(num[i])
              }
            }
          }
        } else {
          withStyle(
            style = SpanStyle(
              color = Color(0xFF5F6368),
              fontWeight = FontWeight.Normal
            )
          ) {
            append(num)
          }
        }
      }

      Text(
        text = annotatedNumber,
        fontSize = 13.sp,
        modifier = Modifier.padding(top = 2.dp)
      )
    }

    // Call icon
    IconButton(
      onClick = onCall,
      modifier = Modifier.testTag("keypad_call_${contact.id}")
    ) {
      Icon(
        imageVector = Icons.Default.Call,
        contentDescription = "Call ${contact.name}",
        tint = Color(0xFF444746),
        modifier = Modifier.size(24.dp)
      )
    }
  }
}
