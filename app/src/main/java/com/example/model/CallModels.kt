package com.example.model

enum class CallType {
  INCOMING,
  OUTGOING,
  MISSED,
  BLOCKED
}

data class CallRecord(
  val id: String,
  val name: String,
  val phoneNumber: String,
  val avatarResId: Int? = null,
  val photoUri: String? = null,
  val type: CallType = CallType.INCOMING,
  val count: Int = 1,
  val simSlot: String = "SIM 1",
  val location: String = "Mobile",
  val timeFormatted: String = "Just now",
  val section: String = "Today",
  val isUnknownNumber: Boolean = false,
  val isSpam: Boolean = false,
  val isFavorite: Boolean = false,
  val timestamp: Long = System.currentTimeMillis()
) {
  val hasPhoto: Boolean
    get() = photoUri != null || avatarResId != null
}

data class Contact(
  val id: String,
  val name: String,
  val phoneNumber: String,
  val avatarResId: Int? = null,
  val photoUri: String? = null,
  val label: String = "Mobile",
  val isFavorite: Boolean = false
) {
  val hasPhoto: Boolean
    get() = photoUri != null || avatarResId != null
}

enum class IncomingGestureMode(val displayName: String) {
  HORIZONTAL_SWIPE("Horizontal swipe"),
  SINGLE_TAP("Single tap")
}

enum class FilterTab(val title: String) {
  ALL("All"),
  MISSED("Missed"),
  CONTACTS("Contacts"),
  NON_SPAM("Non-spam")
}

enum class AudioOutputMode(val label: String) {
  EARPIECE("Phone"),
  SPEAKER("Speaker"),
  BLUETOOTH("Bluetooth")
}

data class ActiveCallState(
  val callerName: String,
  val phoneNumber: String,
  val carrier: String = "SIM 1",
  val avatarResId: Int? = null,
  val photoUri: String? = null,
  val isIncoming: Boolean = false,
  val isConnected: Boolean = false,
  val isMuted: Boolean = false,
  val isHold: Boolean = false,
  val isRecording: Boolean = false,
  val audioOutput: AudioOutputMode = AudioOutputMode.EARPIECE,
  val durationSeconds: Int = 0
) {
  val hasPhoto: Boolean
    get() = photoUri != null || avatarResId != null
}
