package com.ujwal.colai.feature.home

import android.graphics.BitmapFactory
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ujwal.colai.core.data.LogoCacheManager
import com.ujwal.colai.core.model.AIService
import com.ujwal.colai.core.ui.theme.ColAISprings
import com.ujwal.colai.util.rememberHaptics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * EXACT iOS Liquid Glass AI Service Card:
 * - 110dp high rounded glass logo container
 * - 24dp rounded squircle with subtle drop shadow
 * - Translucent liquid glass tint (Color(0xFF1C1C1E) 60% in dark, Color(0xFFE5E5EA) 70% in light)
 * - 0.5px hairline specular border
 * - Logo with 10dp rounded corners and monochrome inversion in dark mode for ChatGPT and Grok
 * - 13sp font-weight 600 service name
 * - 10sp session count subtitle ("1 session" / "2 sessions")
 */
@Composable
fun AIServiceCard(
    service: AIService,
    sessionCount: Int,
    isDark: Boolean,
    onTap: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptics = rememberHaptics()
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1.0f,
        animationSpec = ColAISprings.Snappy,
        label = "CardPressScale"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .pointerInput(service.id) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        haptics.impactLight()
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = {
                        haptics.selection()
                        onTap()
                    },
                    onLongPress = {
                        haptics.impactMedium()
                        onLongPress?.invoke()
                    }
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Liquid Glass Logo Container - Exactly 110dp height
        Box(
            modifier = Modifier
                .height(110.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val containerShape = RoundedCornerShape(24.dp)
            val glassBg = if (isDark) {
                Color(0xFF1C1C1E).copy(alpha = 0.60f)
            } else {
                Color(0xFFE5E5EA).copy(alpha = 0.70f)
            }
            val borderGlint = if (isDark) {
                Color(0x22FFFFFF)
            } else {
                Color(0x08000000)
            }

            Box(
                modifier = Modifier
                    .size(92.dp)
                    .shadow(
                        elevation = 15.dp,
                        shape = containerShape,
                        spotColor = Color.Black.copy(alpha = 0.15f),
                        ambientColor = Color.Black.copy(alpha = 0.10f)
                    )
                    .clip(containerShape)
                    .background(glassBg)
                    .border(width = 0.5.dp, color = borderGlint, shape = containerShape)
                    .padding(18.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .aspectRatio(1f),
                    contentAlignment = Alignment.Center
                ) {
                    ServiceCardIcon(
                        service = service,
                        isDark = isDark
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Service Name (13sp, font weight 600, color #FFFFFF in dark / #000000 in light)
        Text(
            text = service.name,
            style = androidx.compose.ui.text.TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.2).sp
            ),
            color = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        // Session Count Subtitle (if > 0, 10sp font weight 500, color #8E8E93)
        if (sessionCount > 0) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$sessionCount ${if (sessionCount == 1) "session" else "sessions"}",
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = Color(0xFF8E8E93),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Service logo rendering with local cache, dark mode inversion for monochrome logos, and fallback letter avatar.
 */
@Composable
fun ServiceCardIcon(
    service: AIService,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, service.id, service.iconPath) {
        value = withContext(Dispatchers.IO) {
            val path = service.iconPath
            var resolvedBitmap: android.graphics.Bitmap? = null

            // 1. Check direct file path if present (with fallback re-anchoring across devices/user profiles)
            if (!path.isNullOrBlank() && !path.startsWith("http://", ignoreCase = true) && !path.startsWith("https://", ignoreCase = true)) {
                val file = File(path)
                if (file.exists() && file.canRead()) {
                    try {
                        resolvedBitmap = BitmapFactory.decodeFile(file.absolutePath)
                    } catch (_: Exception) {}
                } else {
                    val localFile = if (path.contains("/logos/")) {
                        File(File(context.filesDir, "logos"), file.name)
                    } else {
                        File(context.filesDir, file.name)
                    }
                    if (localFile.exists() && localFile.canRead()) {
                        try {
                            resolvedBitmap = BitmapFactory.decodeFile(localFile.absolutePath)
                        } catch (_: Exception) {}
                    }
                }
            }

            // 2. Check local logo cache directory by service ID
            if (resolvedBitmap == null) {
                val cached = LogoCacheManager(context).getCachedLogoFile(service.id)
                if (cached != null && cached.exists() && cached.canRead()) {
                    try {
                        resolvedBitmap = BitmapFactory.decodeFile(cached.absolutePath)
                    } catch (_: Exception) {}
                }
            }

            // 3. Fallback to bundled resource drawable for default providers
            if (resolvedBitmap == null) {
                val defaultRes = com.ujwal.colai.core.data.DefaultServicesRepository.getDefaultIconResource(service.id)
                if (defaultRes != null) {
                    try {
                        resolvedBitmap = BitmapFactory.decodeResource(context.resources, defaultRes)
                    } catch (_: Exception) {}
                }
            }

            // 4. If still missing and we have network info, asynchronously queue a background download
            if (resolvedBitmap == null) {
                try {
                    LogoCacheManager(context).cacheLogoForService(service)
                    val freshlyCached = LogoCacheManager(context).getCachedLogoFile(service.id)
                    if (freshlyCached != null && freshlyCached.exists()) {
                        resolvedBitmap = BitmapFactory.decodeFile(freshlyCached.absolutePath)
                    }
                } catch (_: Exception) {}
            }

            resolvedBitmap
        }
    }

    // Invert ChatGPT and Grok monochrome logos in dark mode
    val shouldInvert = isDark && (service.id.equals("chatgpt", ignoreCase = true) || service.id.equals("grok", ignoreCase = true))

    val colorFilter = if (shouldInvert) {
        val invertMatrix = ColorMatrix(
            floatArrayOf(
                -1f,  0f,  0f, 0f, 255f,
                 0f, -1f,  0f, 0f, 255f,
                 0f,  0f, -1f, 0f, 255f,
                 0f,  0f,  0f, 1f,   0f
            )
        )
        ColorFilter.colorMatrix(invertMatrix)
    } else {
        null
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = service.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                colorFilter = colorFilter
            )
        } else {
            // Letter Avatar Fallback (32sp thin font, color #8E8E93)
            val initial = service.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            Text(
                text = initial,
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Light,
                    color = Color(0xFF8E8E93)
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Compact service icon component used in headers and lists.
 */
@Composable
fun ServiceIcon(
    service: AIService,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp
) {
    val isDark = isSystemInDarkTheme()
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.22f))
            .background(if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA))
            .padding(size * 0.15f),
        contentAlignment = Alignment.Center
    ) {
        ServiceCardIcon(
            service = service,
            isDark = isDark
        )
    }
}

