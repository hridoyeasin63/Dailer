package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import com.example.model.CallRecord
import com.example.model.CallType
import com.example.model.Contact
import com.example.model.FilterTab
import com.example.model.IncomingGestureMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class CallRepository(private val context: Context) {

  private val prefs: SharedPreferences =
    context.getSharedPreferences("dialer_prefs", Context.MODE_PRIVATE)

  // Empty initial state - NO default mock numbers!
  private val _contacts = MutableStateFlow<List<Contact>>(loadSavedContacts())
  val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

  private val _callHistory = MutableStateFlow<List<CallRecord>>(loadSavedCallHistory())
  val callHistory: StateFlow<List<CallRecord>> = _callHistory.asStateFlow()

  private val _blockedNumbers = MutableStateFlow<Set<String>>(loadBlockedNumbers())
  val blockedNumbers: StateFlow<Set<String>> = _blockedNumbers.asStateFlow()

  private val _gestureMode = MutableStateFlow(loadGestureMode())
  val gestureMode: StateFlow<IncomingGestureMode> = _gestureMode.asStateFlow()

  private val _selectedFilter = MutableStateFlow(FilterTab.ALL)
  val selectedFilter: StateFlow<FilterTab> = _selectedFilter.asStateFlow()

  fun setFilter(tab: FilterTab) {
    _selectedFilter.value = tab
  }

  fun setGestureMode(mode: IncomingGestureMode) {
    _gestureMode.value = mode
    prefs.edit().putString("gesture_mode", mode.name).apply()
  }

  private fun loadGestureMode(): IncomingGestureMode {
    val saved = prefs.getString("gesture_mode", IncomingGestureMode.HORIZONTAL_SWIPE.name)
    return try {
      IncomingGestureMode.valueOf(saved ?: IncomingGestureMode.HORIZONTAL_SWIPE.name)
    } catch (e: Exception) {
      IncomingGestureMode.HORIZONTAL_SWIPE
    }
  }

  /**
   * Syncs real device contacts from ContactsContract if permission is available.
   */
  suspend fun syncDeviceContacts() = withContext(Dispatchers.IO) {
    try {
      val deviceList = mutableListOf<Contact>()
      val contentResolver = context.contentResolver
      val cursor: Cursor? = contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        arrayOf(
          ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
          ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
          ContactsContract.CommonDataKinds.Phone.NUMBER,
          ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
          ContactsContract.CommonDataKinds.Phone.TYPE
        ),
        null,
        null,
        "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
      )

      cursor?.use { c ->
        val idIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
        val nameIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        val numberIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
        val photoIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
        val typeIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)

        val seenNumbers = mutableSetOf<String>()

        while (c.moveToNext()) {
          val name = if (nameIdx >= 0) c.getString(nameIdx) ?: "Unknown" else "Unknown"
          val rawNumber = if (numberIdx >= 0) c.getString(numberIdx) ?: "" else ""
          val cleanNumber = rawNumber.replace(" ", "").replace("-", "")
          if (cleanNumber.isBlank() || seenNumbers.contains(cleanNumber)) continue
          seenNumbers.add(cleanNumber)

          val photoUri = if (photoIdx >= 0) c.getString(photoIdx) else null
          val id = if (idIdx >= 0) c.getString(idIdx) else UUID.randomUUID().toString()

          deviceList.add(
            Contact(
              id = "device_$id",
              name = name,
              phoneNumber = rawNumber,
              photoUri = photoUri,
              label = "Mobile",
              isFavorite = false
            )
          )
        }
      }

      if (deviceList.isNotEmpty()) {
        // Merge with custom saved contacts (custom take precedence)
        val customContacts = loadSavedContacts()
        val customNumbers = customContacts.map { it.phoneNumber.replace(" ", "") }.toSet()
        val combined = customContacts + deviceList.filter {
          !customNumbers.contains(it.phoneNumber.replace(" ", ""))
        }
        _contacts.value = combined.sortedBy { it.name }
      }
    } catch (e: SecurityException) {
      Log.d("CallRepository", "Permission not yet granted for contacts: ${e.message}")
    } catch (e: Exception) {
      Log.e("CallRepository", "Error syncing contacts", e)
    }
  }

  fun addContact(name: String, phoneNumber: String, label: String = "Mobile", photoUri: String? = null) {
    val newContact = Contact(
      id = UUID.randomUUID().toString(),
      name = name,
      phoneNumber = phoneNumber,
      photoUri = photoUri,
      label = label,
      isFavorite = false
    )
    val updated = listOf(newContact) + _contacts.value.filter { it.phoneNumber != phoneNumber }
    _contacts.value = updated
    saveContacts(updated.filter { !it.id.startsWith("device_") })

    // Update call history if unknown number matched
    _callHistory.value = _callHistory.value.map {
      if (it.phoneNumber.replace(" ", "") == phoneNumber.replace(" ", "")) {
        it.copy(name = name, photoUri = photoUri, isUnknownNumber = false)
      } else {
        it
      }
    }
    saveCallHistory(_callHistory.value)
  }

  fun toggleFavorite(contactId: String) {
    _contacts.value = _contacts.value.map {
      if (it.id == contactId) it.copy(isFavorite = !it.isFavorite) else it
    }
  }

  fun blockNumber(number: String) {
    val clean = number.replace(" ", "").replace("-", "")
    val newSet = _blockedNumbers.value + clean
    _blockedNumbers.value = newSet
    saveBlockedNumbers(newSet)
  }

  fun unblockNumber(number: String) {
    val clean = number.replace(" ", "").replace("-", "")
    val newSet = _blockedNumbers.value - clean
    _blockedNumbers.value = newSet
    saveBlockedNumbers(newSet)
  }

  fun isNumberBlocked(number: String): Boolean {
    val clean = number.replace(" ", "").replace("-", "")
    return _blockedNumbers.value.contains(clean)
  }

  fun clearCallHistory() {
    _callHistory.value = emptyList()
    prefs.edit().remove("call_history_json").apply()
  }

  fun deleteCallRecord(id: String) {
    _callHistory.value = _callHistory.value.filter { it.id != id }
    saveCallHistory(_callHistory.value)
  }

  fun recordCall(
    name: String,
    phoneNumber: String,
    type: CallType,
    photoUri: String? = null,
    avatarRes: Int? = null
  ) {
    val matchedContact = findContactByNumber(phoneNumber)
    val finalName = if (name.isNotBlank() && name != phoneNumber) name else (matchedContact?.name ?: phoneNumber)
    val finalPhoto = photoUri ?: matchedContact?.photoUri
    val finalAvatar = avatarRes ?: matchedContact?.avatarResId
    val isUnknown = matchedContact == null && finalName == phoneNumber

    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    val timeFormatted = sdf.format(Date())

    val newRecord = CallRecord(
      id = UUID.randomUUID().toString(),
      name = finalName,
      phoneNumber = phoneNumber,
      avatarResId = finalAvatar,
      photoUri = finalPhoto,
      type = type,
      count = 1,
      simSlot = "SIM 1",
      location = "Mobile",
      timeFormatted = timeFormatted,
      section = "Today",
      isUnknownNumber = isUnknown,
      timestamp = System.currentTimeMillis()
    )

    val updatedList = listOf(newRecord) + _callHistory.value
    _callHistory.value = updatedList
    saveCallHistory(updatedList)
  }

  fun findContactByNumber(number: String): Contact? {
    val clean = number.replace(" ", "").replace("-", "").takeLast(8)
    if (clean.isBlank()) return null
    return _contacts.value.firstOrNull {
      val cClean = it.phoneNumber.replace(" ", "").replace("-", "").takeLast(8)
      cClean.isNotEmpty() && (clean == cClean || clean.endsWith(cClean) || cClean.endsWith(clean))
    }
  }

  // --- Persistence helpers ---
  private fun saveContacts(contacts: List<Contact>) {
    val array = JSONArray()
    for (c in contacts) {
      val obj = JSONObject().apply {
        put("id", c.id)
        put("name", c.name)
        put("number", c.phoneNumber)
        put("label", c.label)
        put("favorite", c.isFavorite)
        c.photoUri?.let { put("photoUri", it) }
      }
      array.put(obj)
    }
    prefs.edit().putString("saved_contacts_json", array.toString()).apply()
  }

  private fun loadSavedContacts(): List<Contact> {
    val json = prefs.getString("saved_contacts_json", null) ?: return emptyList()
    val list = mutableListOf<Contact>()
    try {
      val array = JSONArray(json)
      for (i in 0 until array.length()) {
        val obj = array.getJSONObject(i)
        list.add(
          Contact(
            id = obj.optString("id", UUID.randomUUID().toString()),
            name = obj.optString("name", "Unknown"),
            phoneNumber = obj.optString("number", ""),
            photoUri = if (obj.has("photoUri")) obj.getString("photoUri") else null,
            label = obj.optString("label", "Mobile"),
            isFavorite = obj.optBoolean("favorite", false)
          )
        )
      }
    } catch (e: Exception) {
      Log.e("CallRepository", "Error parsing saved contacts", e)
    }
    return list
  }

  private fun saveCallHistory(history: List<CallRecord>) {
    val array = JSONArray()
    for (r in history.take(100)) {
      val obj = JSONObject().apply {
        put("id", r.id)
        put("name", r.name)
        put("number", r.phoneNumber)
        put("type", r.type.name)
        put("count", r.count)
        put("simSlot", r.simSlot)
        put("location", r.location)
        put("time", r.timeFormatted)
        put("section", r.section)
        put("isUnknown", r.isUnknownNumber)
        put("timestamp", r.timestamp)
        r.photoUri?.let { put("photoUri", it) }
      }
      array.put(obj)
    }
    prefs.edit().putString("call_history_json", array.toString()).apply()
  }

  private fun loadSavedCallHistory(): List<CallRecord> {
    val json = prefs.getString("call_history_json", null) ?: return emptyList()
    val list = mutableListOf<CallRecord>()
    try {
      val array = JSONArray(json)
      for (i in 0 until array.length()) {
        val obj = array.getJSONObject(i)
        val typeStr = obj.optString("type", CallType.INCOMING.name)
        val callType = try { CallType.valueOf(typeStr) } catch (e: Exception) { CallType.INCOMING }
        list.add(
          CallRecord(
            id = obj.optString("id", UUID.randomUUID().toString()),
            name = obj.optString("name", ""),
            phoneNumber = obj.optString("number", ""),
            photoUri = if (obj.has("photoUri")) obj.getString("photoUri") else null,
            type = callType,
            count = obj.optInt("count", 1),
            simSlot = obj.optString("simSlot", "SIM 1"),
            location = obj.optString("location", "Mobile"),
            timeFormatted = obj.optString("time", "Recent"),
            section = obj.optString("section", "Today"),
            isUnknownNumber = obj.optBoolean("isUnknown", false),
            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
          )
        )
      }
    } catch (e: Exception) {
      Log.e("CallRepository", "Error loading call history", e)
    }
    return list
  }

  private fun saveBlockedNumbers(set: Set<String>) {
    val array = JSONArray()
    set.forEach { array.put(it) }
    prefs.edit().putString("blocked_numbers_json", array.toString()).apply()
  }

  private fun loadBlockedNumbers(): Set<String> {
    val json = prefs.getString("blocked_numbers_json", null) ?: return emptySet()
    val set = mutableSetOf<String>()
    try {
      val array = JSONArray(json)
      for (i in 0 until array.length()) {
        set.add(array.getString(i))
      }
    } catch (e: Exception) {
      Log.e("CallRepository", "Error loading blocked numbers", e)
    }
    return set
  }
}
