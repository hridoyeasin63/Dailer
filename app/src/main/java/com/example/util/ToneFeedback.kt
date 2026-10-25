package com.example.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

class ToneFeedback(private val context: Context) {
  private var toneGenerator: ToneGenerator? = null

  init {
    try {
      toneGenerator = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 70)
    } catch (e: Exception) {
      Log.w("ToneFeedback", "Could not initialize ToneGenerator", e)
    }
  }

  fun playKeyTone(key: Char) {
    playHaptic()
    val tone = when (key) {
      '0' -> ToneGenerator.TONE_DTMF_0
      '1' -> ToneGenerator.TONE_DTMF_1
      '2' -> ToneGenerator.TONE_DTMF_2
      '3' -> ToneGenerator.TONE_DTMF_3
      '4' -> ToneGenerator.TONE_DTMF_4
      '5' -> ToneGenerator.TONE_DTMF_5
      '6' -> ToneGenerator.TONE_DTMF_6
      '7' -> ToneGenerator.TONE_DTMF_7
      '8' -> ToneGenerator.TONE_DTMF_8
      '9' -> ToneGenerator.TONE_DTMF_9
      '*' -> ToneGenerator.TONE_DTMF_S
      '#' -> ToneGenerator.TONE_DTMF_P
      else -> ToneGenerator.TONE_PROP_BEEP
    }

    try {
      toneGenerator?.startTone(tone, 120)
    } catch (e: Exception) {
      // Ignored if audio stream unavailable
    }
  }

  fun playHaptic() {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator?.vibrate(
          VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
        )
      } else {
        @Suppress("DEPRECATION")
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          vibrator?.vibrate(VibrationEffect.createOneShot(18, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
          @Suppress("DEPRECATION")
          vibrator?.vibrate(18)
        }
      }
    } catch (e: Exception) {
      // Safe fallback
    }
  }

  fun release() {
    try {
      toneGenerator?.release()
      toneGenerator = null
    } catch (e: Exception) {
      // Ignore
    }
  }
}
