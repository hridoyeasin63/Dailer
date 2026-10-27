package com.example.ui

import android.Manifest
import android.app.Application
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.Call
import android.telecom.TelecomManager
import android.util.Log
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
import com.example.model.SimInfo
import com.example.telecom.AppInCallService
import com.example.util.SimManager
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

data class CallTarget(
  val name: String,
  val phoneNumber: String,
  val avatarRes: Int? = null,
  val photoUri: String? = null
)

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

  // Default dialer status
  private val _isDefaultDialer = MutableStateFlow(false)
  val isDefaultDialer: StateFlow<Boolean> = _isDefaultDialer.asStateFlow()

  // Multi-SIM state & selection
  private val _availableSims = MutableStateFlow<List<SimInfo>>(emptyList())
  val availableSims: StateFlow<List<SimInfo>> = _availableSims.asStateFlow()

  private val _pendingSimCall = MutableStateFlow<CallTarget?>(null)
  val pendingSimCall: StateFlow<CallTarget?> = _pendingSimCall.asStateFlow()

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
    syncContacts()
    syncCallLogs()
    refreshSims()
    checkDefaultDialerStatus()

    // Listen to real telecom calls if app is default phone app
    AppInCallService.onCallAddedListener = { telecomCall ->
      val number = telecomCall.details?.handle?.schemeSpecificPart ?: ""
      val contact = repository.findContactByNumber(number)
      val name = contact?.name ?: number
      val photo = contact?.photoUri
      val avatar = contact?.avatarResId
      val isRinging = telecomCall.state == Call.STATE_RINGING
      val isActive = telecomCall.state == Call.STATE_ACTIVE

      _activeCall.value = ActiveCallState(
        callerName = name,
        phoneNumber = number,
        carrier = "SIM 1",
        avatarResId = avatar,
        photoUri = photo,
        isIncoming = isRinging,
        isConnected = isActive,
        audioOutput = AudioOutputMode.EARPIECE,
        durationSeconds = 0
      )
      if (isActive) {
        startCallTimer()
      }
    }

    AppInCallService.onCallStateChangedListener = { telecomCall, state ->
      Log.d("DialerViewModel", "Telecom Call State Changed: $state")
      when (state) {
        Call.STATE_ACTIVE -> {
          _activeCall.value?.let { current ->
            if (!current.isConnected) {
              _activeCall.value = current.copy(isConnected = true)
              startCallTimer()
            }
          }
        }
        Call.STATE_DISCONNECTED -> {
          endCall()
        }
      }
    }

    AppInCallService.onCallRemovedListener = {
      endCall()
    }
  }

  fun syncContacts() {
    viewModelScope.launch {
      repository.syncDeviceContacts()
    }
  }

  fun syncCallLogs() {
    viewModelScope.launch {
      repository.syncDeviceCallLogs()
    }
  }

  fun refreshSims() {
    _availableSims.value = SimManager.getAvailableSims(getApplication())
  }

  fun checkDefaultDialerStatus() {
    val context = getApplication<Application>()
    val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
    _isDefaultDialer.value = telecomManager?.defaultDialerPackage == context.packageName
  }

  fun getRequestDefaultDialerIntent(): Intent? {
    val context = getApplication<Application>()
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      val roleManager = context.getSystemService(RoleManager::class.java)
      if (roleManager?.isRoleAvailable(RoleManager.ROLE_DIALER) == true && !roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) {
        roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
      } else {
        null
      }
    } else {
      val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
      if (telecomManager?.defaultDialerPackage != context.packageName) {
        Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
          putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
        }
      } else {
        null
      }
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
    if (screen == Screen.SETTINGS) {
      checkDefaultDialerStatus()
    }
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

  // CALL ACTIONS & MULTI-SIM SELECTION
  fun initiateCall(
    name: String,
    phoneNumber: String,
    avatarRes: Int? = null,
    photoUri: String? = null
  ) {
    toneFeedback.playHaptic()
    val cleanNumber = phoneNumber.trim()
    if (cleanNumber.isBlank()) return

    refreshSims()
    val sims = _availableSims.value

    if (sims.size > 1) {
      // Prompt user with SIM selection dialog
      _pendingSimCall.value = CallTarget(name, cleanNumber, avatarRes, photoUri)
    } else {
      executeCall(name, cleanNumber, avatarRes, photoUri, sims.firstOrNull())
    }
  }

  fun confirmCallWithSim(selectedSim: SimInfo) {
    toneFeedback.playHaptic()
    val target = _pendingSimCall.value ?: return
    _pendingSimCall.value = null
    executeCall(target.name, target.phoneNumber, target.avatarRes, target.photoUri, selectedSim)
  }

  fun cancelSimSelection() {
    _pendingSimCall.value = null
  }

  fun startOutgoingCall(
    name: String,
    phoneNumber: String,
    avatarRes: Int? = null,
    photoUri: String? = null
  ) {
    initiateCall(name, phoneNumber, avatarRes, photoUri)
  }

  fun executeCall(
    name: String,
    phoneNumber: String,
    avatarRes: Int? = null,
    photoUri: String? = null,
    selectedSim: SimInfo? = null
  ) {
    val cleanNumber = phoneNumber.trim()
    if (cleanNumber.isBlank()) return

    val contact = repository.findContactByNumber(cleanNumber)
    val finalName = if (name.isNotBlank() && name != cleanNumber) name else (contact?.name ?: cleanNumber)
    val finalPhoto = photoUri ?: contact?.photoUri
    val finalAvatar = avatarRes ?: contact?.avatarResId
    val carrierLabel = selectedSim?.title ?: "SIM 1"

    // 1. Record call log with consecutive grouping rule
    repository.recordCall(finalName, cleanNumber, CallType.OUTGOING, finalPhoto, finalAvatar)

    val context = getApplication<Application>()
    val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
    val callUri = Uri.parse("tel:${Uri.encode(cleanNumber)}")

    // 2. Prepare SIM selection extras
    val simBundle = Bundle().apply {
      if (selectedSim?.phoneAccountHandle != null) {
        putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, selectedSim.phoneAccountHandle)
      }
      if (selectedSim != null) {
        putInt("com.android.phone.extra.slot", selectedSim.slotIndex)
        putInt("simSlot", selectedSim.slotIndex)
        putInt("slot", selectedSim.slotIndex)
        putInt("sim_slot", selectedSim.slotIndex)
        putInt("subscription_id", selectedSim.subscriptionId)
        putInt("android.telephony.extra.SUBSCRIPTION_INDEX", selectedSim.subscriptionId)
      }
    }

    // 3. Handle default dialer vs non-default dialer
    if (_isDefaultDialer.value) {
      // In default dialer mode, show in-call UI in dialing state (NOT connected yet!)
      _activeCall.value = ActiveCallState(
        callerName = finalName,
        phoneNumber = cleanNumber,
        carrier = carrierLabel,
        avatarResId = finalAvatar,
        photoUri = finalPhoto,
        isIncoming = false,
        isConnected = false, // Connecting / Dialing state; real timer starts ONLY when telecom call becomes ACTIVE
        audioOutput = AudioOutputMode.EARPIECE,
        durationSeconds = 0
      )

      try {
        telecomManager?.placeCall(callUri, simBundle)
      } catch (e: Exception) {
        Log.e("DialerViewModel", "telecomManager.placeCall failed, falling back to Intent", e)
        val callIntent = Intent(Intent.ACTION_CALL).apply {
          data = callUri
          putExtras(simBundle)
          flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
          context.startActivity(callIntent)
        } catch (err: Exception) {
          val dialIntent = Intent(Intent.ACTION_DIAL).apply {
            data = callUri
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
          }
          context.startActivity(dialIntent)
        }
      }
    } else {
      // If NOT default dialer, DO NOT show fake internal in-call UI!
      // Delegate cleanly to Android system telephony using ACTION_CALL with selected SIM
      val hasCallPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CALL_PHONE
      ) == PackageManager.PERMISSION_GRANTED

      try {
        if (hasCallPermission) {
          val callIntent = Intent(Intent.ACTION_CALL).apply {
            data = callUri
            putExtras(simBundle)
            if (selectedSim?.phoneAccountHandle != null) {
              putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, selectedSim.phoneAccountHandle)
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
          }
          context.startActivity(callIntent)
        } else {
          val dialIntent = Intent(Intent.ACTION_DIAL).apply {
            data = callUri
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
          }
          context.startActivity(dialIntent)
        }
      } catch (e: Exception) {
        Log.e("DialerViewModel", "Failed to launch call intent", e)
        try {
          val dialIntent = Intent(Intent.ACTION_DIAL).apply {
            data = callUri
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
          }
          context.startActivity(dialIntent)
        } catch (err: Exception) {
          showSnackbar("Calling $cleanNumber")
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
    try {
      AppInCallService.activeTelecomCall?.answer(0)
    } catch (e: Exception) {
      Log.e("DialerViewModel", "Error answering telecom call", e)
    }
    _activeCall.value = _activeCall.value?.copy(
      isIncoming = false,
      isConnected = true
    )
    startCallTimer()
  }

  fun declineIncomingCall() {
    toneFeedback.playHaptic()
    try {
      AppInCallService.activeTelecomCall?.disconnect()
    } catch (e: Exception) {
      Log.e("DialerViewModel", "Error disconnecting telecom call", e)
    }
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
    try {
      AppInCallService.activeTelecomCall?.disconnect()
    } catch (e: Exception) {
      Log.e("DialerViewModel", "Error ending telecom call", e)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      val context = getApplication<Application>()
      if (ContextCompat.checkSelfPermission(context, Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED) {
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        try {
          telecomManager?.endCall()
        } catch (e: Exception) {
          // ignore
        }
      }
    }

    _activeCall.value = null
    _showInCallKeypad.value = false
    syncCallLogs()
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
      val newHold = !it.isHold
      if (newHold) {
        AppInCallService.activeTelecomCall?.hold()
      } else {
        AppInCallService.activeTelecomCall?.unhold()
      }
      it.copy(isHold = newHold)
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
    startOutgoingCall(number, number)
  }

  override fun onCleared() {
    super.onCleared()
    toneFeedback.release()
    callTimerJob?.cancel()
    AppInCallService.onCallAddedListener = null
    AppInCallService.onCallRemovedListener = null
  }
}
