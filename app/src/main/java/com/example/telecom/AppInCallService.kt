package com.example.telecom

import android.content.Intent
import android.telecom.Call
import android.telecom.InCallService
import android.util.Log
import com.example.MainActivity

class AppInCallService : InCallService() {

  companion object {
    var activeTelecomCall: Call? = null
    var onCallAddedListener: ((Call) -> Unit)? = null
    var onCallRemovedListener: ((Call) -> Unit)? = null
    var onCallStateChangedListener: ((Call, Int) -> Unit)? = null
  }

  override fun onCallAdded(call: Call) {
    super.onCallAdded(call)
    Log.d("AppInCallService", "Telecom Call Added: ${call.details?.handle}")
    activeTelecomCall = call
    onCallAddedListener?.invoke(call)

    // Bring MainActivity to the front to handle the in-call or incoming call UI
    try {
      val intent = Intent(this, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        putExtra("from_telecom", true)
      }
      startActivity(intent)
    } catch (e: Exception) {
      Log.e("AppInCallService", "Could not start MainActivity", e)
    }

    call.registerCallback(object : Call.Callback() {
      override fun onStateChanged(call: Call, state: Int) {
        super.onStateChanged(call, state)
        Log.d("AppInCallService", "Call state changed to $state")
        onCallStateChangedListener?.invoke(call, state)
        if (state == Call.STATE_DISCONNECTED) {
          if (activeTelecomCall == call) {
            activeTelecomCall = null
          }
          onCallRemovedListener?.invoke(call)
        }
      }
    })
  }

  override fun onCallRemoved(call: Call) {
    super.onCallRemoved(call)
    Log.d("AppInCallService", "Telecom Call Removed")
    if (activeTelecomCall == call) {
      activeTelecomCall = null
    }
    onCallRemovedListener?.invoke(call)
  }
}
