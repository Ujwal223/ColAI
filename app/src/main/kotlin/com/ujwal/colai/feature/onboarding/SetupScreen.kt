package com.ujwal.colai.feature.onboarding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ujwal.colai.core.data.DefaultServicesRepository
import com.ujwal.colai.core.data.LogoCacheManager
import com.ujwal.colai.core.database.AppDatabase
import com.ujwal.colai.core.security.EncryptedStorage
import com.ujwal.colai.core.ui.components.CupertinoActivityIndicator
import com.ujwal.colai.util.rememberHaptics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * EXACT Cupertino SetupScreen:
 * - OLED black background (#000000)
 * - Cupertino activity indicator (radius 15)
 * - "Setting Things Up" (fontSize 24, font weight 800, letterSpacing -1.0)
 * - Status task text ("Downloading logo for...", "Finishing up...")
 * - Linear progress indicator (width 240, height 4, rounded 3)
 */
@Composable
fun SetupScreen(
    onSetupComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = rememberHaptics()

    var progressTarget by remember { mutableFloatStateOf(0.05f) }
    var currentTask by remember { mutableStateOf("Initializing...") }

    val animatedProgress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = tween(durationMillis = 300),
        label = "SetupProgress"
    )

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val database = AppDatabase.getDatabase(context)
            val storage = EncryptedStorage.getInstance(context)
            val repository = DefaultServicesRepository(database, storage)
            val logoCacheManager = LogoCacheManager(context, database.serviceDao())

            repository.seedIfEmpty()
            val services = database.serviceDao().getAllServicesList()
            val total = services.size.coerceAtLeast(1)

            services.forEachIndexed { index, service ->
                withContext(Dispatchers.Main) {
                    currentTask = "Downloading logo for ${service.name}..."
                    progressTarget = (index.toFloat() / total.toFloat()) * 0.9f
                }
                logoCacheManager.cacheLogoForService(service)
            }

            withContext(Dispatchers.Main) {
                currentTask = "Finishing up..."
                progressTarget = 1.0f
            }

            storage.setOnboardingCompleted(true)
            kotlinx.coroutines.delay(200)
        }

        haptics.success()
        onSetupComplete()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF000000)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CupertinoActivityIndicator(
                    radius = 15.dp,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Setting Things Up",
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = (-1.0).sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = currentTask,
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = 15.sp,
                        color = Color(0xFF98989E),
                        fontWeight = FontWeight.Medium
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(54.dp))

                // Progress Bar (width 240, height 4, rounded 3)
                Box(
                    modifier = Modifier
                        .width(240.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = 0.10f))
                ) {
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxSize(),
                        color = Color.White,
                        trackColor = Color.Transparent
                    )
                }
            }
        }
    }
}
