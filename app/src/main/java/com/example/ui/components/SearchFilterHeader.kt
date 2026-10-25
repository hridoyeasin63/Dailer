package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FilterTab
import com.example.ui.theme.ChipSelectedBg
import com.example.ui.theme.ChipSelectedText
import com.example.ui.theme.ChipUnselectedBg
import com.example.ui.theme.ChipUnselectedBorder
import com.example.ui.theme.ChipUnselectedText
import com.example.ui.theme.PhoneBlue

@Composable
fun SearchFilterHeader(
  searchQuery: String,
  onSearchChange: (String) -> Unit,
  selectedFilter: FilterTab,
  onFilterSelected: (FilterTab) -> Unit,
  onMenuClick: () -> Unit,
  onViewContactsClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp)
  ) {
    // 1. Search Bar (Rounded pill shape like Google Phone)
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .height(54.dp)
        .shadow(elevation = 2.dp, shape = CircleShape)
        .testTag("search_bar_container"),
      shape = CircleShape,
      color = Color.White
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = onMenuClick,
          modifier = Modifier.testTag("menu_drawer_button")
        ) {
          Icon(
            imageVector = Icons.Default.Menu,
            contentDescription = "Menu",
            tint = Color(0xFF444746)
          )
        }

        Box(
          modifier = Modifier
            .weight(1f)
            .padding(horizontal = 4.dp),
          contentAlignment = Alignment.CenterStart
        ) {
          if (searchQuery.isEmpty()) {
            Text(
              text = "Search contacts",
              color = Color(0xFF747775),
              fontSize = 16.sp
            )
          }
          TextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            colors = TextFieldDefaults.colors(
              focusedContainerColor = Color.Transparent,
              unfocusedContainerColor = Color.Transparent,
              disabledContainerColor = Color.Transparent,
              focusedIndicatorColor = Color.Transparent,
              unfocusedIndicatorColor = Color.Transparent,
              focusedTextColor = Color(0xFF1F1F1F),
              unfocusedTextColor = Color(0xFF1F1F1F)
            ),
            singleLine = true,
            modifier = Modifier.testTag("search_text_input")
          )
        }

        if (searchQuery.isNotEmpty()) {
          IconButton(onClick = { onSearchChange("") }) {
            Icon(
              imageVector = Icons.Default.Clear,
              contentDescription = "Clear search",
              tint = Color(0xFF444746)
            )
          }
        } else {
          IconButton(
            onClick = { /* Mic action */ },
            modifier = Modifier.testTag("mic_search_button")
          ) {
            Icon(
              imageVector = Icons.Default.Mic,
              contentDescription = "Voice search",
              tint = Color(0xFF444746)
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 2. Filter chips row (All, Missed, Contacts, Non-spam)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      FilterTab.values().forEach { tab ->
        val isSelected = tab == selectedFilter
        val bgColor = if (isSelected) ChipSelectedBg else ChipUnselectedBg
        val textColor = if (isSelected) ChipSelectedText else ChipUnselectedText
        val borderColor = if (isSelected) Color.Transparent else ChipUnselectedBorder

        Box(
          modifier = Modifier
            .clip(CircleShape)
            .background(bgColor)
            .border(width = 1.dp, color = borderColor, shape = CircleShape)
            .clickable { onFilterSelected(tab) }
            .padding(horizontal = 20.dp, vertical = 9.dp)
            .testTag("filter_chip_${tab.name.lowercase()}"),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = tab.title,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 3. Favorites dropdown & View contacts button
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .clickable { /* Favorites filter */ }
          .padding(vertical = 4.dp)
          .testTag("favorites_dropdown")
      ) {
        Text(
          text = "Favorites",
          color = PhoneBlue,
          fontSize = 15.sp,
          fontWeight = FontWeight.Medium
        )
        Icon(
          imageVector = Icons.Default.ArrowDropDown,
          contentDescription = "Favorites menu",
          tint = PhoneBlue,
          modifier = Modifier.size(22.dp)
        )
      }

      // "View contacts" rounded button
      Surface(
        onClick = onViewContactsClick,
        shape = CircleShape,
        color = Color(0xFFE8EEF4),
        modifier = Modifier.testTag("view_contacts_button")
      ) {
        Text(
          text = "View contacts",
          color = Color(0xFF1F1F1F),
          fontSize = 13.sp,
          fontWeight = FontWeight.Medium,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
      }
    }
  }
}
