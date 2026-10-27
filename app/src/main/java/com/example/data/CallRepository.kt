package com.example.data

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import android.util.Log
import androidx.core.content.ContextCompat
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
import java.util.Calendar
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

  /**
   * Syncs real system call logs from CallLog.Calls.
   * Consecutive calls to the same phone number are grouped together with count incremented.
   */
  suspend fun syncDeviceCallLogs() = withContext(Dispatchers.IO) {
    val hasPermission = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.READ_CALL_LOG
    ) == PackageManager.PERMISSION_GRANTED

    if (!hasPermission) {
      Log.d("CallRepository", "READ_CALL_LOG permission not yet granted")
      return@withContext
    }

    try {
      val projection = arrayOf(
        CallLog.Calls._ID,
        CallLog.Calls.NUMBER,
        CallLog.Calls.CACHED_NAME,
        CallLog.Calls.TYPE,
        CallLog.Calls.DATE,
        CallLog.Calls.DURATION
      )

      val cursor = context.contentResolver.query(
        CallLog.Calls.CONTENT_URI,
        projection,
        null,
        null,
        "${CallLog.Calls.DATE} DESC"
      )

      data class RawCall(
        val id: String,
        val number: String,
        val cachedName: String?,
        val typeInt: Int,
        val timestamp: Long,
        val duration: Long
      )

      val rawList = mutableListOf<RawCall>()
      cursor?.use { c ->
        val idIdx = c.getColumnIndex(CallLog.Calls._ID)
        val numIdx = c.getColumnIndex(CallLog.Calls.NUMBER)
        val nameIdx = c.getColumnIndex(CallLog.Calls.CACHED_NAME)
        val typeIdx = c.getColumnIndex(CallLog.Calls.TYPE)
        val dateIdx = c.getColumnIndex(CallLog.Calls.DATE)
        val durIdx = c.getColumnIndex(CallLog.Calls.DURATION)

        while (c.moveToNext()) {
          val number = if (numIdx >= 0) c.getString(numIdx) ?: "" else ""
          if (number.isBlank()) continue
          val id = if (idIdx >= 0) c.getString(idIdx) ?: UUID.randomUUID().toString() else UUID.randomUUID().toString()
          val cachedName = if (nameIdx >= 0) c.getString(nameIdx) else null
          val typeInt = if (typeIdx >= 0) c.getInt(typeIdx) else CallLog.Calls.INCOMING_TYPE
          val timestamp = if (dateIdx >= 0) c.getLong(dateIdx) else System.currentTimeMillis()
          val duration = if (durIdx >= 0) c.getLong(durIdx) else 0L

          rawList.add(RawCall(id, number, cachedName, typeInt, timestamp, duration))
        }
      }

      if (rawList.isNotEmpty()) {
        val groupedResult = mutableListOf<CallRecord>()
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

        for (raw in rawList) {
          val lastGroup = groupedResult.lastOrNull()
          if (lastGroup != null && isSamePhoneNumber(lastGroup.phoneNumber, raw.number)) {
            // Group consecutive calls to the same number together
            val updated = lastGroup.copy(count = lastGroup.count + 1)
            groupedResult[groupedResult.lastIndex] = updated
          } else {
            val contact = findContactByNumber(raw.number)
            val displayName = when {
              !raw.cachedName.isNullOrBlank() -> raw.cachedName
              contact != null -> contact.name
              else -> raw.number
            }
            val photoUri = contact?.photoUri
            val avatarRes = contact?.avatarResId
            val isUnknown = contact == null && (raw.cachedName.isNullOrBlank() || displayName == raw.number)

            val callType = when (raw.typeInt) {
              CallLog.Calls.OUTGOING_TYPE -> CallType.OUTGOING
              CallLog.Calls.MISSED_TYPE, CallLog.Calls.REJECTED_TYPE -> CallType.MISSED
              CallLog.Calls.BLOCKED_TYPE -> CallType.BLOCKED
              else -> CallType.INCOMING
            }

            groupedResult.add(
              CallRecord(
                id = "sys_${raw.id}",
                name = displayName,
                phoneNumber = raw.number,
                avatarResId = avatarRes,
                photoUri = photoUri,
                type = callType,
                count = 1,
                simSlot = "SIM 1",
                location = "Mobile",
                timeFormatted = timeFormat.format(Date(raw.timestamp)),
                section = formatSectionHeader(raw.timestamp),
                isUnknownNumber = isUnknown,
                timestamp = raw.timestamp
              )
            )
          }
        }

        _callHistory.value = groupedResult
        saveCallHistory(groupedResult)
      }
    } catch (e: SecurityException) {
      Log.d("CallRepository", "READ_CALL_LOG security exception: ${e.message}")
    } catch (e: Exception) {
      Log.e("CallRepository", "Error querying CallLog.Calls", e)
    }
  }

  private fun formatSectionHeader(timestamp: Long): String {
    val calNow = Calendar.getInstance()
    val calCall = Calendar.getInstance().apply { timeInMillis = timestamp }

    val isSameDay = calNow.get(Calendar.YEAR) == calCall.get(Calendar.YEAR) &&
      calNow.get(Calendar.DAY_OF_YEAR) == calCall.get(Calendar.DAY_OF_YEAR)

    if (isSameDay) return "Today"

    calNow.add(Calendar.DAY_OF_YEAR, -1)
    val isYesterday = calNow.get(Calendar.YEAR) == calCall.get(Calendar.YEAR) &&
      calNow.get(Calendar.DAY_OF_YEAR) == calCall.get(Calendar.DAY_OF_YEAR)

    if (isYesterday) return "Yesterday"

    val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
    return sdf.format(Date(timestamp))
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

  private fun isSamePhoneNumber(num1: String, num2: String): Boolean {
    val clean1 = num1.filter { it.isDigit() }
    val clean2 = num2.filter { it.isDigit() }
    if (clean1.isEmpty() || clean2.isEmpty()) return num1.trim() == num2.trim()
    if (clean1 == clean2) return true
    val minLen = minOf(clean1.length, clean2.length)
    if (minLen >= 7) {
      val sub1 = clean1.takeLast(minLen)
      val sub2 = clean2.takeLast(minLen)
      if (sub1 == sub2) return true
    }
    return false
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

    val currentList = _callHistory.value
    val latestRecord = currentList.firstOrNull()

    // Rule:
    // If consecutive calls to the SAME number -> group into the single top record and increment count (e.g. 2, 3...)
    // If another number was called in between, or this is a different number -> add as a new separate entry!
    val updatedList = if (latestRecord != null && isSamePhoneNumber(latestRecord.phoneNumber, phoneNumber)) {
      val updatedLatest = latestRecord.copy(
        count = latestRecord.count + 1,
        timeFormatted = timeFormatted,
        type = type,
        name = if (name.isNotBlank() && name != phoneNumber) name else latestRecord.name,
        photoUri = finalPhoto ?: latestRecord.photoUri,
        avatarResId = finalAvatar ?: latestRecord.avatarResId,
        timestamp = System.currentTimeMillis()
      )
      listOf(updatedLatest) + currentList.drop(1)
    } else {
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
      listOf(newRecord) + currentList
    }

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
