package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.Screen
import com.example.ui.theme.PhoneBlueDark
import com.example.ui.theme.PhoneBlueLight

@Composable
fun DialerBottomBar(
  currentScreen: Screen,
  onScreenSelect: (Screen) -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    color = Color(0xFFF1F5F9),
    shadowElevation = 4.dp
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .height(72.dp),
      horizontalArrangement = Arrangement.SpaceEvenly,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Home tab
      val isHome = currentScreen == Screen.HOME
      Column(
        modifier = Modifier
          .clickable { onScreenSelect(Screen.HOME) }
          .padding(horizontal = 24.dp, vertical = 6.dp)
          .testTag("nav_tab_home"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Box(
          modifier = Modifier
            .width(64.dp)
            .height(32.dp)
            .clip(CircleShape)
            .background(if (isHome) PhoneBlueLight else Color.Transparent),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = if (isHome) Icons.Filled.Home else Icons.Outlined.Home,
            contentDescription = "Home",
            tint = if (isHome) PhoneBlueDark else Color(0xFF444746),
            modifier = Modifier.size(24.dp)
          )
        }
        Text(
          text = "Home",
          fontSize = 12.sp,
          fontWeight = if (isHome) FontWeight.Bold else FontWeight.Medium,
          color = if (isHome) PhoneBlueDark else Color(0xFF444746),
          modifier = Modifier.padding(top = 2.dp)
        )
      }

      // Keypad tab
      val isKeypad = currentScreen == Screen.KEYPAD
      Column(
        modifier = Modifier
          .clickable { onScreenSelect(Screen.KEYPAD) }
          .padding(horizontal = 24.dp, vertical = 6.dp)
          .testTag("nav_tab_keypad"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Box(
          modifier = Modifier
            .width(64.dp)
            .height(32.dp)
            .clip(CircleShape)
            .background(if (isKeypad) PhoneBlueLight else Color.Transparent),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Dialpad,
            contentDescription = "Keypad",
            tint = if (isKeypad) PhoneBlueDark else Color(0xFF444746),
            modifier = Modifier.size(24.dp)
          )
        }
        Text(
          text = "Keypad",
          fontSize = 12.sp,
          fontWeight = if (isKeypad) FontWeight.Bold else FontWeight.Medium,
          color = if (isKeypad) PhoneBlueDark else Color(0xFF444746),
          modifier = Modifier.padding(top = 2.dp)
        )
      }
    }
  }
}
