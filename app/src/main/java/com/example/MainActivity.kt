package com.example

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.DialerViewModel
import com.example.ui.Screen
import com.example.ui.components.AppDrawerSheet
import com.example.ui.components.DialerBottomBar
import com.example.ui.dialogs.AddContactDialog
import com.example.ui.dialogs.IncomingGestureDialog
import com.example.ui.screens.ActiveCallScreen
import com.example.ui.screens.BlockedNumbersScreen
import com.example.ui.screens.ContactsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.IncomingCallScreen
import com.example.ui.screens.KeypadScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

  private val viewModel: DialerViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    handleDialIntent(intent)

    setContent {
      MyApplicationTheme {
        DialerApp(viewModel = viewModel)
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    handleDialIntent(intent)
  }

  override fun onResume() {
    super.onResume()
    viewModel.checkDefaultDialerStatus()
  }

  private fun handleDialIntent(intent: Intent?) {
    val uri = intent?.data
    if (uri?.scheme == "tel") {
      val number = uri.schemeSpecificPart
      if (!number.isNullOrBlank()) {
        viewModel.setScreen(Screen.KEYPAD)
        viewModel.onKeypadClear()
        for (ch in number) {
          viewModel.onKeypadPress(ch)
        }
      }
    }
  }
}

@Composable
fun DialerApp(viewModel: DialerViewModel) {
  val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  val currentScreen by viewModel.currentScreen.collectAsState()
  val callHistory by viewModel.callHistory.collectAsState()
  val contacts by viewModel.contacts.collectAsState()
  val blockedNumbers by viewModel.blockedNumbers.collectAsState()
  val gestureMode by viewModel.gestureMode.collectAsState()
  val selectedFilter by viewModel.selectedFilter.collectAsState()
  val searchQuery by viewModel.searchQuery.collectAsState()
  val keypadInput by viewModel.keypadInput.collectAsState()
  val t9Matches by viewModel.t9Matches.collectAsState()
  val activeCall by viewModel.activeCall.collectAsState()
  val isDefaultDialer by viewModel.isDefaultDialer.collectAsState()
  val showGestureDialog by viewModel.showGestureDialog.collectAsState()
  val addContactPrefill by viewModel.addContactPrefill.collectAsState()
  val showInCallKeypad by viewModel.showInCallKeypad.collectAsState()
  val snackbarMessage by viewModel.snackbarMessage.collectAsState()

  // Default dialer request launcher
  val defaultDialerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
  ) {
    viewModel.checkDefaultDialerStatus()
  }

  // Request permissions on launch for real device contacts and calls
  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { permissions ->
    if (permissions[Manifest.permission.READ_CONTACTS] == true) {
      viewModel.syncContacts()
    }
    viewModel.checkDefaultDialerStatus()
  }

  LaunchedEffect(Unit) {
    permissionLauncher.launch(
      arrayOf(
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.CALL_PHONE
      )
    )
  }

  // Display snackbars
  LaunchedEffect(snackbarMessage) {
    snackbarMessage?.let {
      snackbarHostState.showSnackbar(it)
      viewModel.clearSnackbar()
    }
  }

  // Handle back press
  BackHandler(enabled = activeCall != null || currentScreen != Screen.HOME || drawerState.isOpen) {
    when {
      drawerState.isOpen -> scope.launch { drawerState.close() }
      activeCall != null -> viewModel.endCall()
      currentScreen != Screen.HOME -> viewModel.setScreen(Screen.HOME)
    }
  }

  // Active call overlay or normal app view
  Box(modifier = Modifier.fillMaxSize()) {
    ModalNavigationDrawer(
      drawerState = drawerState,
      gesturesEnabled = activeCall == null && currentScreen == Screen.HOME,
      drawerContent = {
        AppDrawerSheet(
          onNavigate = { screen -> viewModel.setScreen(screen) },
          onClearHistory = { viewModel.clearCallHistory() },
          onSimulateIncomingCallWithPhoto = {
            viewModel.triggerSimulatedIncomingCall(
              name = "Better Half",
              phoneNumber = "+880 1886-406968",
              photoUri = null
            )
          },
          onSimulateIncomingCallNoPhoto = {
            viewModel.triggerSimulatedIncomingCall(
              name = "John Doe (No Photo)",
              phoneNumber = "+880 1711-000000",
              photoUri = null
            )
          },
          onClose = { scope.launch { drawerState.close() } }
        )
      }
    ) {
      Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
          if (activeCall == null && (currentScreen == Screen.HOME || currentScreen == Screen.KEYPAD)) {
            DialerBottomBar(
              currentScreen = currentScreen,
              onScreenSelect = { viewModel.setScreen(it) }
            )
          }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF1F5F9)
      ) { paddingValues ->
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
        ) {
          AnimatedContent(
            targetState = currentScreen,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "screen_transition"
          ) { target ->
            when (target) {
              Screen.HOME -> HomeScreen(
                callHistory = callHistory,
                searchQuery = searchQuery,
                onSearchChange = { viewModel.setSearchQuery(it) },
                selectedFilter = selectedFilter,
                onFilterChange = { viewModel.setFilter(it) },
                onCallContact = { name, number, avatar, photoUri ->
                  viewModel.startOutgoingCall(name, number, avatar, photoUri)
                },
                onAddContact = { number -> viewModel.openAddContact(number) },
                onBlockNumber = { number -> viewModel.blockNumber(number) },
                onOpenMenu = { scope.launch { drawerState.open() } },
                onViewContacts = { viewModel.setScreen(Screen.CONTACTS) },
                onOpenKeypad = { viewModel.setScreen(Screen.KEYPAD) },
                isDefaultDialer = isDefaultDialer,
                onRequestDefaultDialer = {
                  val requestIntent = viewModel.getRequestDefaultDialerIntent()
                  if (requestIntent != null) {
                    defaultDialerLauncher.launch(requestIntent)
                  } else {
                    viewModel.showSnackbar("This app is already your default dialer")
                  }
                }
              )

              Screen.KEYPAD -> KeypadScreen(
                keypadInput = keypadInput,
                t9Matches = t9Matches,
                onDigitPress = { viewModel.onKeypadPress(it) },
                onBackspace = { viewModel.onKeypadBackspace() },
                onClear = { viewModel.onKeypadClear() },
                onCall = { name, number, avatar, photoUri ->
                  viewModel.startOutgoingCall(name, number, avatar, photoUri)
                },
                onAddContact = { number -> viewModel.openAddContact(number) }
              )

              Screen.CONTACTS -> ContactsScreen(
                contacts = contacts,
                onBack = { viewModel.setScreen(Screen.HOME) },
                onAddContact = { viewModel.openAddContact() },
                onSyncContacts = { viewModel.syncContacts() },
                onCall = { name, number, avatar, photoUri ->
                  viewModel.startOutgoingCall(name, number, avatar, photoUri)
                },
                onToggleFavorite = { viewModel.toggleFavorite(it) }
              )

              Screen.SETTINGS -> SettingsScreen(
                gestureMode = gestureMode,
                isDefaultDialer = isDefaultDialer,
                onRequestDefaultDialer = {
                  val requestIntent = viewModel.getRequestDefaultDialerIntent()
                  if (requestIntent != null) {
                    defaultDialerLauncher.launch(requestIntent)
                  } else {
                    viewModel.showSnackbar("This app is already your default dialer")
                  }
                },
                onOpenGestureDialog = { viewModel.setShowGestureDialog(true) },
                onBack = { viewModel.setScreen(Screen.HOME) },
                onOpenBlockedNumbers = { viewModel.setScreen(Screen.BLOCKED_NUMBERS) }
              )

              Screen.BLOCKED_NUMBERS -> BlockedNumbersScreen(
                blockedNumbers = blockedNumbers,
                onUnblock = { viewModel.unblockNumber(it) },
                onBlockNew = { viewModel.blockNumber(it) },
                onBack = { viewModel.setScreen(Screen.SETTINGS) }
              )
            }
          }
        }
      }
    }

    // OVERLAY: Incoming or Active call
    activeCall?.let { call ->
      if (call.isIncoming) {
        IncomingCallScreen(
          callState = call,
          gestureMode = gestureMode,
          onAnswer = { viewModel.answerIncomingCall() },
          onDecline = { viewModel.declineIncomingCall() },
          onSendMessage = { viewModel.sendQuickMessage(it) }
        )
      } else {
        ActiveCallScreen(
          callState = call,
          showInCallKeypad = showInCallKeypad,
          onToggleKeypad = { viewModel.toggleInCallKeypad() },
          onToggleMute = { viewModel.toggleMute() },
          onCycleAudio = { viewModel.cycleAudioOutput() },
          onToggleRecord = { viewModel.toggleRecording() },
          onToggleHold = { viewModel.toggleHold() },
          onEndCall = { viewModel.endCall() },
          onKeypadDigit = { viewModel.toneFeedback.playKeyTone(it) }
        )
      }
    }

    // DIALOG: Incoming Call Gesture dialog
    if (showGestureDialog) {
      IncomingGestureDialog(
        currentMode = gestureMode,
        onSelectMode = { viewModel.setGestureMode(it) },
        onDismiss = { viewModel.setShowGestureDialog(false) }
      )
    }

    // DIALOG: Add contact dialog
    addContactPrefill?.let { prefill ->
      AddContactDialog(
        initialNumber = prefill,
        onSave = { name, number, label, photoUri ->
          viewModel.saveNewContact(name, number, label, photoUri)
        },
        onDismiss = { viewModel.closeAddContact() }
      )
    }
  }
}
