package com.example.ui.screens

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.Contact
import com.example.ui.theme.BackgroundLight
import com.example.ui.theme.PhoneBlue

@Composable
fun ContactsScreen(
  contacts: List<Contact>,
  onBack: () -> Unit,
  onAddContact: () -> Unit,
  onSyncContacts: () -> Unit,
  onCall: (name: String, number: String, avatar: Int?, photoUri: String?) -> Unit,
  onToggleFavorite: (contactId: String) -> Unit,
  modifier: Modifier = Modifier
) {
  var query by remember { mutableStateOf("") }

  val filteredContacts = remember(contacts, query) {
    if (query.isBlank()) {
      contacts.sortedBy { it.name }
    } else {
      contacts.filter {
        it.name.contains(query, ignoreCase = true) ||
          it.phoneNumber.replace(" ", "").contains(query.replace(" ", ""))
      }.sortedBy { it.name }
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(BackgroundLight)
      .statusBarsPadding()
      .testTag("contacts_screen")
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
        modifier = Modifier.testTag("contacts_back_button")
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = "Back",
          tint = Color(0xFF1F1F1F)
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = "Contacts (${contacts.size})",
        fontSize = 20.sp,
        fontWeight = FontWeight.Normal,
        color = Color(0xFF1F1F1F),
        modifier = Modifier.weight(1f)
      )

      // Sync button
      IconButton(
        onClick = onSyncContacts,
        modifier = Modifier.testTag("sync_contacts_button")
      ) {
        Icon(
          imageVector = Icons.Default.Sync,
          contentDescription = "Sync Device Contacts",
          tint = Color(0xFF444746)
        )
      }

      // Add contact button
      IconButton(
        onClick = onAddContact,
        modifier = Modifier.testTag("add_contact_fab")
      ) {
        Icon(
          imageVector = Icons.Default.Add,
          contentDescription = "Add Contact",
          tint = PhoneBlue
        )
      }
    }

    // Search input
    OutlinedTextField(
      value = query,
      onValueChange = { query = it },
      placeholder = { Text("Search contacts...") },
      shape = CircleShape,
      colors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        focusedBorderColor = PhoneBlue,
        unfocusedBorderColor = Color(0xFFDADCE0)
      ),
      singleLine = true,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 6.dp)
        .testTag("contacts_search_input")
    )

    Spacer(modifier = Modifier.height(6.dp))

    // List of contacts
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
        .padding(horizontal = 16.dp, vertical = 8.dp),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = Color.White),
      elevation = CardDefaults.cardElevation(0.5.dp)
    ) {
      if (filteredContacts.isEmpty()) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            tint = Color(0xFFB0B5BA),
            modifier = Modifier.size(56.dp)
          )
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = if (query.isEmpty()) "No contacts saved" else "No matching contact",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF444746)
          )
          Spacer(modifier = Modifier.height(16.dp))
          Button(
            onClick = onAddContact,
            colors = ButtonDefaults.buttonColors(containerColor = PhoneBlue)
          ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Add new contact")
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .testTag("contacts_lazy_list")
        ) {
          items(filteredContacts, key = { it.id }) { contact ->
            ContactListItem(
              contact = contact,
              onCall = { onCall(contact.name, contact.phoneNumber, contact.avatarResId, contact.photoUri) },
              onToggleFavorite = { onToggleFavorite(contact.id) }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ContactListItem(
  contact: Contact,
  onCall: () -> Unit,
  onToggleFavorite: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onCall() }
      .padding(horizontal = 16.dp, vertical = 12.dp)
      .testTag("contact_item_${contact.id}"),
    verticalAlignment = Alignment.CenterVertically
  ) {
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

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = contact.name,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        color = Color(0xFF1F1F1F)
      )
      Text(
        text = "${contact.label} • ${contact.phoneNumber}",
        fontSize = 13.sp,
        color = Color(0xFF5F6368),
        modifier = Modifier.padding(top = 2.dp)
      )
    }

    // Favorite star
    IconButton(onClick = onToggleFavorite) {
      Icon(
        imageVector = if (contact.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
        contentDescription = "Favorite",
        tint = if (contact.isFavorite) Color(0xFFF9AB00) else Color(0xFF747775),
        modifier = Modifier.size(22.dp)
      )
    }

    // Call icon
    IconButton(onClick = onCall) {
      Icon(
        imageVector = Icons.Default.Call,
        contentDescription = "Call ${contact.name}",
        tint = Color(0xFF444746),
        modifier = Modifier.size(22.dp)
      )
    }
  }
}
