package com.example.ui.components

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Report
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.CallRecord
import com.example.model.CallType
import com.example.ui.theme.CallRed
import com.example.ui.theme.PhoneBlueText

@Composable
fun CallHistoryCard(
  record: CallRecord,
  onCallClick: () -> Unit,
  onAddContactClick: () -> Unit,
  onBlockClick: () -> Unit,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 4.dp)
      .testTag("call_record_${record.id}"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clickable { onClick() }
        .padding(14.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Avatar
        if (record.photoUri != null) {
          AsyncImage(
            model = Uri.parse(record.photoUri),
            contentDescription = record.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
              .size(46.dp)
              .clip(CircleShape)
          )
        } else if (record.avatarResId != null) {
          Image(
            painter = painterResource(id = record.avatarResId),
            contentDescription = record.name,
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
              modifier = Modifier.size(28.dp)
            )
          }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Info
        Column(modifier = Modifier.weight(1f)) {
          // Name + count
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = record.name,
              fontSize = 16.sp,
              fontWeight = FontWeight.Medium,
              color = Color(0xFF1F1F1F)
            )
            if (record.count > 1) {
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "(${record.count})",
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF444746)
              )
            }
          }

          Spacer(modifier = Modifier.height(3.dp))

          // Call direction arrow + subtitle
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            when (record.type) {
              CallType.MISSED -> {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.CallMissed,
                  contentDescription = "Missed call",
                  tint = CallRed,
                  modifier = Modifier.size(16.dp)
                )
              }
              CallType.INCOMING -> {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.CallReceived,
                  contentDescription = "Incoming call",
                  tint = CallRed,
                  modifier = Modifier.size(16.dp)
                )
              }
              CallType.OUTGOING -> {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.CallMade,
                  contentDescription = "Outgoing call",
                  tint = Color(0xFF444746),
                  modifier = Modifier.size(16.dp)
                )
              }
              CallType.BLOCKED -> {
                Icon(
                  imageVector = Icons.Default.Report,
                  contentDescription = "Blocked call",
                  tint = CallRed,
                  modifier = Modifier.size(16.dp)
                )
              }
            }

            val subtitleText = buildString {
              if (record.location.isNotEmpty()) {
                append("${record.location} • ")
              }
              append(record.timeFormatted)
            }

            Text(
              text = subtitleText,
              fontSize = 13.sp,
              color = Color(0xFF444746)
            )
          }

          // Carrier tag
          Text(
            text = record.simSlot,
            fontSize = 13.sp,
            color = PhoneBlueText,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.padding(top = 2.dp)
          )
        }

        // Call icon button
        IconButton(
          onClick = onCallClick,
          modifier = Modifier
            .size(44.dp)
            .testTag("call_button_${record.id}")
        ) {
          Icon(
            imageVector = Icons.Default.Call,
            contentDescription = "Call ${record.name}",
            tint = Color(0xFF444746),
            modifier = Modifier.size(24.dp)
          )
        }
      }

      // Quick action chips for unknown numbers
      if (record.isUnknownNumber) {
        Spacer(modifier = Modifier.height(10.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Add contact button
          Surface(
            onClick = onAddContactClick,
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFEFF2F6),
            modifier = Modifier
              .weight(1f)
              .testTag("add_contact_chip_${record.id}")
          ) {
            Row(
              modifier = Modifier.padding(vertical = 8.dp),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.PersonAdd,
                contentDescription = null,
                tint = Color(0xFF1F1F1F),
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Add contact",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1F1F1F)
              )
            }
          }

          // Block or report button
          Surface(
            onClick = onBlockClick,
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFEFF2F6),
            modifier = Modifier
              .weight(1f)
              .testTag("block_chip_${record.id}")
          ) {
            Row(
              modifier = Modifier.padding(vertical = 8.dp),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Report,
                contentDescription = null,
                tint = CallRed,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Block or report",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1F1F1F)
              )
            }
          }
        }
      }
    }
  }
}
