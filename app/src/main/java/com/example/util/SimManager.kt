package com.example.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat
import com.example.model.SimInfo

object SimManager {

  fun getAvailableSims(context: Context): List<SimInfo> {
    val hasPermission = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.READ_PHONE_STATE
    ) == PackageManager.PERMISSION_GRANTED

    if (!hasPermission) {
      return emptyList()
    }

    return try {
      val subscriptionManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
        context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
      } else {
        null
      }

      val activeSubscriptions: List<SubscriptionInfo>? = subscriptionManager?.activeSubscriptionInfoList
      if (!activeSubscriptions.isNullOrEmpty()) {
        activeSubscriptions.map { sub ->
          val slot = if (sub.simSlotIndex >= 0) sub.simSlotIndex else 0
          SimInfo(
            slotIndex = slot,
            subscriptionId = sub.subscriptionId,
            displayName = sub.displayName?.toString() ?: "SIM ${slot + 1}",
            carrierName = sub.carrierName?.toString() ?: "",
            number = try {
              sub.number
            } catch (e: Exception) {
              null
            }
          )
        }.sortedBy { it.slotIndex }
      } else {
        emptyList()
      }
    } catch (e: Exception) {
      emptyList()
    }
  }
}
