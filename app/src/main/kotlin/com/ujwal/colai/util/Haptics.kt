package com.ujwal.colai.util

import android.content.Context
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

/**
 * Unified Tactile Haptic Controller delivering iOS Taptic Engine-like feedback.
 * Utilizes hardware VibrationEffect predefined primitives on API 29+ with graceful fallback
 * to View.performHapticFeedback on older devices.
 */
class Haptics(
    private val context: Context? = null,
    private val view: View? = null
) {
    private val vibrator: Vibrator? by lazy {
        if (context == null) null
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    /**
     * Subtle micro-tick on selection change, segmented pill tab toggle, or slider tick.
     */
    fun selection() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && vibrator?.hasVibrator() == true) {
            try {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                return
            } catch (_: Exception) {}
        }
        view?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    /**
     * Light impact on standard button tap, chip selection, or switch toggle.
     */
    fun impactLight() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && vibrator?.hasVibrator() == true) {
            try {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                return
            } catch (_: Exception) {}
        }
        view?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    /**
     * Medium crisp impact on modal presentation or account switch.
     */
    fun impactMedium() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && vibrator?.hasVibrator() == true) {
            try {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
                return
            } catch (_: Exception) {}
        }
        view?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    /**
     * Heavy impact on long-press or destructive confirmation.
     */
    fun impactHeavy() {
        view?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    /**
     * Success signature pulse on action completion or clean login.
     */
    fun success() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && vibrator?.hasVibrator() == true) {
            try {
                val timings = longArrayOf(0, 30, 60, 40)
                val amplitudes = intArrayOf(0, 100, 0, 180)
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                return
            } catch (_: Exception) {}
        }
        view?.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
    }

    /**
     * Error / rejection multi-pulse.
     */
    fun error() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && vibrator?.hasVibrator() == true) {
            try {
                val timings = longArrayOf(0, 35, 40, 35, 40, 35)
                val amplitudes = intArrayOf(0, 200, 0, 200, 0, 200)
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                return
            } catch (_: Exception) {}
        }
        view?.performHapticFeedback(HapticFeedbackConstants.REJECT)
    }
}

/**
 * Convenience Compose remember helper for accessing the [Haptics] controller.
 */
@Composable
fun rememberHaptics(): Haptics {
    val context = LocalContext.current
    val view = LocalView.current
    return remember(context, view) {
        Haptics(context = context, view = view)
    }
}
