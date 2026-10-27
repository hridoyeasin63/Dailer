package com.example.model

data class SimInfo(
  val slotIndex: Int,
  val subscriptionId: Int,
  val displayName: String,
  val carrierName: String,
  val number: String? = null
) {
  val title: String
    get() = if (displayName.isNotBlank() && !displayName.equals(carrierName, ignoreCase = true)) {
      "$displayName ($carrierName)"
    } else if (carrierName.isNotBlank()) {
      "SIM ${slotIndex + 1} • $carrierName"
    } else {
      "SIM ${slotIndex + 1}"
    }
}
