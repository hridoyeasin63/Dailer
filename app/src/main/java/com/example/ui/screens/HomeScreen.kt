package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CallRecord
import com.example.model.CallType
import com.example.model.FilterTab
import com.example.ui.components.CallHistoryCard
import com.example.ui.components.SearchFilterHeader
import com.example.ui.theme.BackgroundLight
import com.example.ui.theme.PhoneBlue
import com.example.ui.theme.PhoneBlueText

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
  callHistory: List<CallRecord>,
  searchQuery: String,
  onSearchChange: (String) -> Unit,
  selectedFilter: FilterTab,
  onFilterChange: (FilterTab) -> Unit,
  onCallContact: (name: String, number: String, avatar: Int?, photoUri: String?) -> Unit,
  onAddContact: (number: String) -> Unit,
  onBlockNumber: (number: String) -> Unit,
  onOpenMenu: () -> Unit,
  onViewContacts: () -> Unit,
  onOpenKeypad: () -> Unit,
  modifier: Modifier = Modifier
) {
  val filteredRecords = remember(callHistory, searchQuery, selectedFilter) {
    callHistory.filter { record ->
      val matchesQuery = if (searchQuery.isBlank()) true else {
        record.name.contains(searchQuery, ignoreCase = true) ||
          record.phoneNumber.replace(" ", "").contains(searchQuery.replace(" ", ""))
      }

      val matchesFilter = when (selectedFilter) {
        FilterTab.ALL -> true
        FilterTab.MISSED -> record.type == CallType.MISSED
        FilterTab.CONTACTS -> !record.isUnknownNumber
        FilterTab.NON_SPAM -> !record.isSpam
      }

      matchesQuery && matchesFilter
    }
  }

  val grouped = remember(filteredRecords) {
    filteredRecords.groupBy { it.section }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(BackgroundLight)
      .statusBarsPadding()
      .testTag("home_call_log_screen")
  ) {
    SearchFilterHeader(
      searchQuery = searchQuery,
      onSearchChange = onSearchChange,
      selectedFilter = selectedFilter,
      onFilterSelected = onFilterChange,
      onMenuClick = onOpenMenu,
      onViewContactsClick = onViewContacts
    )

    if (filteredRecords.isEmpty()) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Icon(
          imageVector = Icons.Default.History,
          contentDescription = null,
          tint = Color(0xFFB0B5BA),
          modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
          text = if (searchQuery.isNotEmpty()) "No matching calls found" else "No recent calls",
          fontSize = 17.sp,
          fontWeight = FontWeight.Medium,
          color = Color(0xFF444746)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = if (searchQuery.isNotEmpty()) "Try searching for another name or number" else "Your call logs will appear here after you make or receive calls",
          fontSize = 13.sp,
          color = Color(0xFF747775),
          textAlign = TextAlign.Center
        )
        if (searchQuery.isEmpty()) {
          Spacer(modifier = Modifier.height(20.dp))
          Button(
            onClick = onOpenKeypad,
            colors = ButtonDefaults.buttonColors(containerColor = PhoneBlue)
          ) {
            Icon(imageVector = Icons.Default.Dialpad, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Open Keypad")
          }
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .testTag("call_log_list")
      ) {
        grouped.forEach { (section, records) ->
          item(key = "header_$section") {
            Text(
              text = section,
              fontSize = 14.sp,
              fontWeight = FontWeight.Medium,
              color = PhoneBlueText,
              modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .testTag("section_header_$section")
            )
          }

          items(records, key = { it.id }) { record ->
            CallHistoryCard(
              record = record,
              onCallClick = {
                onCallContact(record.name, record.phoneNumber, record.avatarResId, record.photoUri)
              },
              onAddContactClick = {
                onAddContact(record.phoneNumber)
              },
              onBlockClick = {
                onBlockNumber(record.phoneNumber)
              },
              onClick = {
                onCallContact(record.name, record.phoneNumber, record.avatarResId, record.photoUri)
              }
            )
          }

          item(key = "spacer_$section") {
            Spacer(modifier = Modifier.height(10.dp))
          }
        }

        item {
          Spacer(modifier = Modifier.height(80.dp))
        }
      }
    }
  }
}
