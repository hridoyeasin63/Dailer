package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CallRepository
import com.example.model.ActiveCallState
import com.example.model.AudioOutputMode
import com.example.model.CallRecord
import com.example.model.CallType
import com.example.model.Contact
import com.example.model.FilterTab
import com.example.model.IncomingGestureMode
import com.example.util.T9MatchResult
import com.example.util.T9Matcher
import com.example.util.ToneFeedback
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class Screen {
  HOME,
  KEYPAD,
  CONTACTS,
  SETTINGS,
  BLOCKED_NUMBERS
}

class DialerViewModel(application: Application) : AndroidViewModel(application) {

  private val repository = CallRepository(application)
  val toneFeedback = ToneFeedback(application)
  private val audioManager = application.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

  val contacts = repository.contacts
  val callHistory = repository.callHistory
  val blockedNumbers = repository.blockedNumbers
  val gestureMode = repository.gestureMode
  val selectedFilter = repository.selectedFilter

  private val _currentScreen = MutableStateFlow(Screen.HOME)
  val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _keypadInput = MutableStateFlow("")
  val keypadInput: StateFlow<String> = _keypadInput.asStateFlow()

  // Active or incoming call state
  private val _activeCall = MutableStateFlow<ActiveCallState?>(null)
  val activeCall: StateFlow<ActiveCallState?> = _activeCall.asStateFlow()

  // Dialogs
  private val _showGestureDialog = MutableStateFlow(false)
  val showGestureDialog: StateFlow<Boolean> = _showGestureDialog.asStateFlow()

  private val _addContactPrefill = MutableStateFlow<String?>(null)
  val addContactPrefill: StateFlow<String?> = _addContactPrefill.asStateFlow()

  private val _showInCallKeypad = MutableStateFlow(false)
  val showInCallKeypad: StateFlow<Boolean> = _showInCallKeypad.asStateFlow()

  private val _snackbarMessage = MutableStateFlow<String?>(null)
  val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

  private var callTimerJob: Job? = null

  init {
    // Sync device contacts if permission already granted
    syncContacts()
  }

  fun syncContacts() {
    viewModelScope.launch {
      repository.syncDeviceContacts()
    }
  }

  // T9 matches computed reactively
  val t9Matches: StateFlow<List<T9MatchResult>> = combine(_keypadInput, contacts) { input, contactList ->
    if (input.isEmpty()) {
      contactList.take(20).map { T9MatchResult(contact = it, score = 0) }
    } else {
      contactList.mapNotNull { T9Matcher.match(it, input) }
        .sortedByDescending { it.score }
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  fun setScreen(screen: Screen) {
    _currentScreen.value = screen
  }

  fun setSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun setFilter(filter: FilterTab) {
    repository.setFilter(filter)
  }

  fun onKeypadPress(digit: Char) {
    toneFeedback.playKeyTone(digit)
    _keypadInput.value = _keypadInput.value + digit
  }

  fun onKeypadBackspace() {
    toneFeedback.playHaptic()
    if (_keypadInput.value.isNotEmpty()) {
      _keypadInput.value = _keypadInput.value.dropLast(1)
    }
  }

  fun onKeypadClear() {
    toneFeedback.playHaptic()
    _keypadInput.value = ""
  }

  fun setGestureMode(mode: IncomingGestureMode) {
    repository.setGestureMode(mode)
    _showGestureDialog.value = false
  }

  fun setShowGestureDialog(show: Boolean) {
    _showGestureDialog.value = show
  }

  fun openAddContact(number: String? = null) {
    _addContactPrefill.value = number
  }

  fun closeAddContact() {
    _addContactPrefill.value = null
  }

  fun saveNewContact(name: String, number: String, label: String = "Mobile", photoUri: String? = null) {
    repository.addContact(name, number, label, photoUri)
    closeAddContact()
    showSnackbar("Contact saved: $name")
  }

  fun blockNumber(number: String) {
    repository.blockNumber(number)
    showSnackbar("Number blocked: $number")
  }

  fun unblockNumber(number: String) {
    repository.unblockNumber(number)
    showSnackbar("Number unblocked: $number")
  }

  fun toggleFavorite(contactId: String) {
    repository.toggleFavorite(contactId)
  }

  fun clearCallHistory() {
    repository.clearCallHistory()
    showSnackbar("Call history cleared")
  }

  fun deleteCallRecord(id: String) {
    repository.deleteCallRecord(id)
    showSnackbar("Call record removed")
  }

  fun showSnackbar(msg: String) {
    _snackbarMessage.value = msg
  }

  fun clearSnackbar() {
    _snackbarMessage.value = null
  }

  // CALL ACTIONS
  fun startOutgoingCall(
    name: String,
    phoneNumber: String,
    avatarRes: Int? = null,
    photoUri: String? = null
  ) {
    toneFeedback.playHaptic()
    val contact = repository.findContactByNumber(phoneNumber)
    val finalName = if (name.isNotBlank() && name != phoneNumber) name else (contact?.name ?: phoneNumber)
    val finalPhoto = photoUri ?: contact?.photoUri
    val finalAvatar = avatarRes ?: contact?.avatarResId

    // Record call log immediately as OUTGOING
    repository.recordCall(finalName, phoneNumber, CallType.OUTGOING, finalPhoto, finalAvatar)

    _activeCall.value = ActiveCallState(
      callerName = finalName,
      phoneNumber = phoneNumber,
      carrier = "SIM 1",
      avatarResId = finalAvatar,
      photoUri = finalPhoto,
      isIncoming = false,
      isConnected = false,
      audioOutput = AudioOutputMode.EARPIECE,
      durationSeconds = 0
    )

    // Simulate call connecting after 1.5s
    viewModelScope.launch {
      delay(1500)
      _activeCall.value?.let { current ->
        if (!current.isConnected) {
          _activeCall.value = current.copy(isConnected = true)
          startCallTimer()
        }
      }
    }
  }

  fun triggerSimulatedIncomingCall(
    name: String? = null,
    phoneNumber: String = "+880 1886-406968",
    photoUri: String? = null
  ) {
    val contact = repository.findContactByNumber(phoneNumber)
    val finalName = name ?: contact?.name ?: "Unknown Caller"
    val finalPhoto = photoUri ?: contact?.photoUri
    val finalAvatar = contact?.avatarResId

    _activeCall.value = ActiveCallState(
      callerName = finalName,
      phoneNumber = phoneNumber,
      carrier = "SIM 1",
      avatarResId = finalAvatar,
      photoUri = finalPhoto,
      isIncoming = true,
      isConnected = false,
      audioOutput = AudioOutputMode.EARPIECE,
      durationSeconds = 0
    )
  }

  fun answerIncomingCall() {
    toneFeedback.playHaptic()
    _activeCall.value = _activeCall.value?.copy(
      isIncoming = false,
      isConnected = true
    )
    startCallTimer()
  }

  fun declineIncomingCall() {
    toneFeedback.playHaptic()
    _activeCall.value?.let { call ->
      repository.recordCall(call.callerName, call.phoneNumber, CallType.MISSED, call.photoUri, call.avatarResId)
    }
    endCall()
  }

  fun sendQuickMessage(text: String) {
    toneFeedback.playHaptic()
    val number = _activeCall.value?.phoneNumber ?: ""
    declineIncomingCall()

    if (number.isNotBlank()) {
      try {
        val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
          data = Uri.parse("smsto:$number")
          putExtra("sms_body", text)
          flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        getApplication<Application>().startActivity(smsIntent)
      } catch (e: Exception) {
        showSnackbar("Sent message: \"$text\"")
      }
    } else {
      showSnackbar("Sent message: \"$text\"")
    }
  }

  fun endCall() {
    toneFeedback.playHaptic()
    callTimerJob?.cancel()
    callTimerJob = null
    _activeCall.value = null
    _showInCallKeypad.value = false
  }

  fun toggleMute() {
    toneFeedback.playHaptic()
    _activeCall.value = _activeCall.value?.let {
      val newMute = !it.isMuted
      audioManager?.isMicrophoneMute = newMute
      it.copy(isMuted = newMute)
    }
  }

  fun toggleHold() {
    toneFeedback.playHaptic()
    _activeCall.value = _activeCall.value?.let {
      it.copy(isHold = !it.isHold)
    }
  }

  fun toggleRecording() {
    toneFeedback.playHaptic()
    _activeCall.value = _activeCall.value?.let {
      val newRec = !it.isRecording
      showSnackbar(if (newRec) "Call recording started" else "Call recording saved")
      it.copy(isRecording = newRec)
    }
  }

  fun cycleAudioOutput() {
    toneFeedback.playHaptic()
    _activeCall.value = _activeCall.value?.let { call ->
      val next = when (call.audioOutput) {
        AudioOutputMode.EARPIECE -> {
          audioManager?.isSpeakerphoneOn = true
          AudioOutputMode.SPEAKER
        }
        AudioOutputMode.SPEAKER -> {
          audioManager?.isSpeakerphoneOn = false
          AudioOutputMode.BLUETOOTH
        }
        AudioOutputMode.BLUETOOTH -> {
          audioManager?.isSpeakerphoneOn = false
          AudioOutputMode.EARPIECE
        }
      }
      showSnackbar("Audio output: ${next.label}")
      call.copy(audioOutput = next)
    }
  }

  fun toggleInCallKeypad() {
    _showInCallKeypad.value = !_showInCallKeypad.value
  }

  private fun startCallTimer() {
    callTimerJob?.cancel()
    callTimerJob = viewModelScope.launch {
      while (true) {
        delay(1000)
        _activeCall.value = _activeCall.value?.let {
          it.copy(durationSeconds = it.durationSeconds + 1)
        } ?: break
      }
    }
  }

  fun dialRealDeviceNumber(number: String) {
    val clean = number.trim()
    if (clean.isBlank()) return
    try {
      val intent = Intent(Intent.ACTION_DIAL).apply {
        data = Uri.parse("tel:$clean")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
      getApplication<Application>().startActivity(intent)
      repository.recordCall(clean, clean, CallType.OUTGOING)
    } catch (e: Exception) {
      startOutgoingCall(clean, clean)
    }
  }

  override fun onCleared() {
    super.onCleared()
    toneFeedback.release()
    callTimerJob?.cancel()
  }
}
