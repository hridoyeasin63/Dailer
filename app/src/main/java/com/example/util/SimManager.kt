package com.example.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.model.SimInfo

object SimManager {

  private const val TAG = "SimManager"

  fun getAvailableSims(context: Context): List<SimInfo> {
    val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
    val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager

    // Check device physical/eSIM slot count
    val hardwareSimCount = try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        telephonyManager?.activeModemCount ?: 1
      } else {
        @Suppress("DEPRECATION")
        telephonyManager?.phoneCount ?: 1
      }
    } catch (e: Exception) {
      1
    }

    val hasStatePerm = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.READ_PHONE_STATE
    ) == PackageManager.PERMISSION_GRANTED

    val hasNumberPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_PHONE_NUMBERS
      ) == PackageManager.PERMISSION_GRANTED
    } else {
      true
    }

    val callCapableAccounts: List<PhoneAccountHandle> = try {
      if (hasStatePerm) {
        telecomManager?.callCapablePhoneAccounts ?: emptyList()
      } else {
        emptyList()
      }
    } catch (e: Exception) {
      Log.d(TAG, "Error getting callCapablePhoneAccounts: ${e.message}")
      emptyList()
    }

    val foundSims = mutableListOf<SimInfo>()

    // Try SubscriptionManager
    if (hasStatePerm || hasNumberPerm) {
      try {
        val subscriptionManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
          context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
        } else {
          null
        }

        val activeList: List<SubscriptionInfo>? = subscriptionManager?.activeSubscriptionInfoList
        if (!activeList.isNullOrEmpty()) {
          activeList.forEach { sub ->
            val slot = if (sub.simSlotIndex >= 0) sub.simSlotIndex else foundSims.size
            val matchedHandle = callCapableAccounts.getOrNull(slot) ?: callCapableAccounts.firstOrNull {
              it.id.contains(sub.subscriptionId.toString())
            }

            val carrier = sub.carrierName?.toString().orEmpty()
            val display = sub.displayName?.toString().orEmpty()
            val name = when {
              display.isNotBlank() && !display.equals("CARD $slot", ignoreCase = true) -> display
              carrier.isNotBlank() -> carrier
              else -> "SIM ${slot + 1}"
            }

            foundSims.add(
              SimInfo(
                slotIndex = slot,
                subscriptionId = sub.subscriptionId,
                displayName = name,
                carrierName = carrier,
                number = try { sub.number } catch (e: Exception) { null },
                phoneAccountHandle = matchedHandle
              )
            )
          }
        }
      } catch (e: Exception) {
        Log.e(TAG, "SubscriptionManager query failed", e)
      }
    }

    // If telecom accounts were found but subscription manager was empty or missing accounts
    if (foundSims.isEmpty() && callCapableAccounts.isNotEmpty()) {
      callCapableAccounts.forEachIndexed { index, handle ->
        val account = try { telecomManager?.getPhoneAccount(handle) } catch (e: Exception) { null }
        val label = account?.label?.toString() ?: "SIM ${index + 1}"
        foundSims.add(
          SimInfo(
            slotIndex = index,
            subscriptionId = index + 1,
            displayName = label,
            carrierName = label,
            number = account?.address?.schemeSpecificPart,
            phoneAccountHandle = handle
          )
        )
      }
    }

    // If hardware clearly has 2 SIM slots (dual-SIM phone) but only 1 or 0 were detected
    if (hardwareSimCount >= 2) {
      if (foundSims.isEmpty()) {
        foundSims.add(
          SimInfo(
            slotIndex = 0,
            subscriptionId = 1,
            displayName = "SIM 1",
            carrierName = "Slot 1",
            number = null,
            phoneAccountHandle = callCapableAccounts.getOrNull(0)
          )
        )
        foundSims.add(
          SimInfo(
            slotIndex = 1,
            subscriptionId = 2,
            displayName = "SIM 2",
            carrierName = "Slot 2",
            number = null,
            phoneAccountHandle = callCapableAccounts.getOrNull(1)
          )
        )
      } else if (foundSims.size == 1) {
        val existingSlot = foundSims[0].slotIndex
        val otherSlot = if (existingSlot == 0) 1 else 0
        foundSims.add(
          SimInfo(
            slotIndex = otherSlot,
            subscriptionId = otherSlot + 1,
            displayName = "SIM ${otherSlot + 1}",
            carrierName = "Slot ${otherSlot + 1}",
            number = null,
            phoneAccountHandle = callCapableAccounts.getOrNull(otherSlot)
          )
        )
      }
    }

    return foundSims.sortedBy { it.slotIndex }
  }
}
