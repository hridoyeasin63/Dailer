package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BackgroundLight
import com.example.ui.theme.CallRed
import com.example.ui.theme.PhoneBlue

@Composable
fun BlockedNumbersScreen(
  blockedNumbers: Set<String>,
  onUnblock: (String) -> Unit,
  onBlockNew: (String) -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  var newNumber by remember { mutableStateOf("") }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(BackgroundLight)
      .statusBarsPadding()
      .testTag("blocked_numbers_screen")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(60.dp)
        .padding(horizontal = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(onClick = onBack, modifier = Modifier.testTag("blocked_back_button")) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = "Back",
          tint = Color(0xFF1F1F1F)
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      Text(
        text = "Blocked numbers",
        fontSize = 22.sp,
        fontWeight = FontWeight.Normal,
        color = Color(0xFF1F1F1F)
      )
    }

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Text(
        text = "You will not receive calls or messages from blocked numbers.",
        fontSize = 14.sp,
        color = Color(0xFF5F6368)
      )

      Spacer(modifier = Modifier.height(16.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedTextField(
          value = newNumber,
          onValueChange = { newNumber = it },
          placeholder = { Text("Add a phone number") },
          singleLine = true,
          modifier = Modifier
            .weight(1f)
            .testTag("add_block_number_input")
        )
        Spacer(modifier = Modifier.width(10.dp))
        Button(
          onClick = {
            if (newNumber.isNotBlank()) {
              onBlockNew(newNumber.trim())
              newNumber = ""
            }
          },
          enabled = newNumber.isNotBlank(),
          colors = ButtonDefaults.buttonColors(containerColor = PhoneBlue),
          modifier = Modifier.testTag("add_block_button")
        ) {
          Text("Block")
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      Card(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.5.dp)
      ) {
        if (blockedNumbers.isEmpty()) {
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
          ) {
            Text("No blocked numbers", color = Color(0xFF747775))
          }
        } else {
          LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(blockedNumbers.toList(), key = { it }) { number ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.Block,
                  contentDescription = null,
                  tint = CallRed,
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                  text = number,
                  fontSize = 16.sp,
                  color = Color(0xFF1F1F1F),
                  modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onUnblock(number) }) {
                  Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Unblock $number",
                    tint = Color(0xFF747775),
                    modifier = Modifier.size(20.dp)
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}
