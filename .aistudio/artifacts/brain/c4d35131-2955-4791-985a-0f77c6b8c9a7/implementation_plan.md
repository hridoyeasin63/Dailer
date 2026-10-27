# Implementation Plan: Real Cellular Calling, Device Call Log Sync, & Multi-SIM Selection

This plan addresses the requirement to place real cellular phone calls (removing artificial timers), load past call records from the Android system call log, and provide a SIM card selection sheet when dialing.

---

## 1. System Call Log Synchronization (`CallLog.Calls`)
- **Permissions**: Add `android.permission.READ_PHONE_STATE` to `AndroidManifest.xml` alongside existing `READ_CALL_LOG` and `WRITE_CALL_LOG`.
- **Permission Flow in `MainActivity.kt`**: Request `READ_CALL_LOG`, `CALL_PHONE`, `READ_CONTACTS`, and `READ_PHONE_STATE` on application start.
- **Data Layer (`CallRepository.kt`)**:
  - Implement `syncDeviceCallLogs()`: Query `android.provider.CallLog.Calls.CONTENT_URI`.
  - Extract call date, number, cached name, duration, and call type (`INCOMING_TYPE`, `OUTGOING_TYPE`, `MISSED_TYPE`, `REJECTED_TYPE`, `BLOCKED_TYPE`).
  - Resolve contact photos from `ContactsContract` for each number.
  - Apply the consecutive call grouping rule (consecutive calls to the same number are consolidated with incremented count `(2)`, `(3)`; a call to a different number starts a new group).
  - Automatically invoke `syncDeviceCallLogs()` in `DialerViewModel.init` and whenever the app resumes.

---

## 2. Multi-SIM Detection & Selection Dialog
- **SIM Model & Repository**:
  - Create `SimAccount(slotIndex: Int, subscriptionId: Int, displayName: String, carrierName: String)`.
  - Use `SubscriptionManager.getActiveSubscriptionInfoList()` to query active SIM cards.
- **UI Component (`SimSelectionSheet.kt`)**:
  - Material 3 Modal Bottom Sheet / Dialog showing each active SIM card with its carrier name (e.g. *SIM 1 - Grameenphone*, *SIM 2 - Banglalink*).
  - Automatically pops up only when multiple active SIM cards are detected before dialing. If only one SIM is active, dials immediately without prompt.
- **Telecom & Intent Injection**:
  - Pass the selected `subscriptionId` or `PhoneAccountHandle` into the call intent:
    - `putExtra("android.telecom.extra.PHONE_ACCOUNT_HANDLE", phoneAccountHandle)`
    - `putExtra("subscription", subscriptionId)`
    - `putExtra("simSlot", slotIndex)`

---

## 3. Real Cellular Call Execution (Removing Mock Delay)
- **Eliminate Mock Delay**:
  - Remove the synthetic `delay(2000)` auto-connect simulation timer from `DialerViewModel.kt`.
- **Native Telecom Integration**:
  - Use `TelecomManager.placeCall(uri, bundle)` when the app holds the default dialer role, binding directly to `AppInCallService`.
  - Fall back to `Intent(Intent.ACTION_CALL)` with the target SIM extras when initiating normal cellular calls.
  - Link real telecom call states (`STATE_DIALING`, `STATE_RINGING`, `STATE_ACTIVE`, `STATE_DISCONNECTED`) via `AppInCallService` to dynamically update `_activeCall` duration, status, and termination.

---

## 4. Verification Plan
1. **Compilation & Linting**: Run `compile_applet` to ensure Kotlin and Android SDK compatibility.
2. **Robolectric Tests**: Execute `gradle :app:testDebugUnitTest` to verify:
   - Call log grouping and date formatting.
   - Dual-SIM detection helper logic.
3. **Manual Flow Check**:
   - Verify previous call logs appear on the home screen.
   - Verify tapping a contact or dialing a number prompts SIM selection if multiple SIMs exist, then launches the cellular call.
