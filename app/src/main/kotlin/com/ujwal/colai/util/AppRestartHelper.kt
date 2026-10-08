package com.ujwal.colai.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.util.Log
import kotlin.system.exitProcess

/**
 * Utility to cleanly restart the ColAI application process.
 * Essential when restoring GeckoView profiles, cookies, and SQLite databases
 * so the native Gecko engine re-initializes fresh from disk without stale in-memory locks.
 */
object AppRestartHelper {
    private const val TAG = "AppRestartHelper"

    fun restartApp(context: Context) {
        try {
            val packageManager = context.packageManager
            val launchIntent = packageManager.getLaunchIntentForPackage(context.packageName)
            val activity = findActivity(context)
            if (launchIntent != null) {
                val component = launchIntent.component
                val restartIntent = Intent.makeRestartActivityTask(component).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                context.startActivity(restartIntent)
                Log.i(TAG, "Triggered restart intent, finishing activity and delaying process kill...")
                activity?.finishAffinity()
                Handler(Looper.getMainLooper()).postDelayed({
                    try {
                        Process.killProcess(Process.myPid())
                        exitProcess(0)
                    } catch (t: Throwable) {
                        Log.w(TAG, "Error exiting process", t)
                    }
                }, 500L)
            } else {
                Log.e(TAG, "Cannot resolve launch intent for package: ${context.packageName}")
                activity?.finishAffinity()
                Handler(Looper.getMainLooper()).postDelayed({
                    Process.killProcess(Process.myPid())
                    exitProcess(0)
                }, 300L)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restart application", e)
            findActivity(context)?.finishAffinity()
            try {
                Process.killProcess(Process.myPid())
            } catch (t: Throwable) {
                // ignore
            }
        }
    }

    private fun findActivity(context: Context): Activity? {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }
}
