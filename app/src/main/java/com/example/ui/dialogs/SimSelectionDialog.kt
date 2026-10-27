package com.example.ui.dialogs

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
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import com.example.model.SimInfo
import com.example.ui.theme.PhoneBlue

@Composable
fun SimSelectionDialog(
  recipientName: String,
  recipientNumber: String,
  simList: List<SimInfo>,
  onSelectSim: (SimInfo) -> Unit,
  onDismiss: () -> Unit
) {
  val simColors = listOf(
    Color(0xFF1A73E8), // Google Blue
    Color(0xFF1E8E3E), // Green
    Color(0xFFE37400), // Orange
    Color(0xFF9334E6)  // Purple
  )

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Column {
        Text(
          text = "কল করার সিম বাছাই করুন",
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = Color(0xFF1F1F1F)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = if (recipientName.isNotBlank() && recipientName != recipientNumber) {
            "$recipientName ($recipientNumber)"
          } else {
            recipientNumber
          },
          fontSize = 13.sp,
          color = Color(0xFF5F6368)
        )
      }
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        simList.forEachIndexed { index, sim ->
          val color = simColors.getOrElse(index % simColors.size) { PhoneBlue }
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp)
              .clickable { onSelectSim(sim) }
              .testTag("sim_select_option_${sim.slotIndex}"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F3F4)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(42.dp)
                  .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.SimCard,
                  contentDescription = null,
                  tint = color,
                  modifier = Modifier.size(24.dp)
                )
              }
              Spacer(modifier = Modifier.width(14.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "SIM ${sim.slotIndex + 1}",
                  fontSize = 15.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = Color(0xFF202124)
                )
                if (sim.carrierName.isNotBlank()) {
                  Text(
                    text = sim.carrierName,
                    fontSize = 13.sp,
                    color = Color(0xFF5F6368)
                  )
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {},
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("বাতিল", color = Color(0xFF5F6368))
      }
    },
    shape = RoundedCornerShape(20.dp),
    containerColor = Color.White
  )
}
