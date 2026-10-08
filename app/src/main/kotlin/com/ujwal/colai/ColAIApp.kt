package com.ujwal.colai

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Process
import android.util.Log
import com.ujwal.colai.core.data.DefaultServicesRepository
import com.ujwal.colai.core.data.LegacyDataMigrator
import com.ujwal.colai.core.engine.GeckoRuntimeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Main application class for ColAI.
 * Initializes core singletons:
 * - Mozilla GeckoRuntime engine
 * - Room Database & Legacy data migration
 * - Default AI service container seeding
 *
 * CRITICAL: GeckoView spawns isolated child processes (e.g. :gpu_disable_art_image_, :tab_disable_art_image_*).
 * Initialization MUST be restricted to the main process only, or child processes crash with signal 9.
 */
class ColAIApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // Check if this is the main process before initializing singletons
        if (!isMainProcess(this)) {
            val processName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                getProcessName()
            } else {
                "child"
            }
            Log.d(TAG, "Running in Gecko child process ($processName); skipping ColAIApp main initialization.")
            return
        }

        Log.i(TAG, "ColAI Main Process starting up...")
        instance = this

        try {
            GeckoRuntimeManager.init(this)
            Log.i(TAG, "GeckoRuntimeManager initialized successfully on app launch.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize GeckoRuntimeManager during app onCreate", e)
        }

        // Initialize and seed data asynchronously
        applicationScope.launch {
            try {
                val migrationResult = LegacyDataMigrator.create(this@ColAIApp).migrateIfNeeded()
                if (migrationResult.isMigrated) {
                    Log.i(TAG, "Migrated ${migrationResult.servicesMigrated} services and ${migrationResult.sessionsMigrated} sessions.")
                }

                val seeded = DefaultServicesRepository.create(this@ColAIApp).seedIfEmpty()
                if (seeded > 0) {
                    Log.i(TAG, "Seeded $seeded default AI services with container sessions.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize persistence and seed repository on launch", e)
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        if (isMainProcess(this)) {
            GeckoRuntimeManager.shutdown()
        }
    }

    companion object {
        private const val TAG = "ColAIApp"

        @Volatile
        lateinit var instance: ColAIApp
            private set

        fun isMainProcess(context: Context): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                return context.packageName == Application.getProcessName()
            }
            val myPid = Process.myPid()
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            activityManager?.runningAppProcesses?.forEach { processInfo ->
                if (processInfo.pid == myPid) {
                    return context.packageName == processInfo.processName
                }
            }
            return true
        }
    }
}
