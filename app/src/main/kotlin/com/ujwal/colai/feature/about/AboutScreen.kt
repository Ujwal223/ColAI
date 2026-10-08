package com.ujwal.colai.feature.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ujwal.colai.BuildConfig
import com.ujwal.colai.R
import com.ujwal.colai.core.ui.components.CupertinoActiveBlue
import com.ujwal.colai.core.ui.components.CupertinoDarkBg
import com.ujwal.colai.core.ui.components.CupertinoFormRow
import com.ujwal.colai.core.ui.components.CupertinoFormSection
import com.ujwal.colai.core.ui.components.CupertinoLightBg
import com.ujwal.colai.core.ui.components.CupertinoNavigationBar
import com.ujwal.colai.core.ui.components.CupertinoSystemGrey
import com.ujwal.colai.core.ui.theme.LocalContrastLevel
import com.ujwal.colai.util.rememberHaptics

/**
 * 100% Native Kotlin & Jetpack Compose Cupertino AboutScreen:
 * - App icon 96x96 with Apple Squircle elevation
 * - Version v2.0.0 Native Kotlin Edition
 * - Grouped Cupertino Section: SPECIFICATIONS
 * - Grouped Cupertino Section: DEVELOPER & SOURCE
 * - Grouped Cupertino Section: LEGAL
 */
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit,
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val isHighContrast = LocalContrastLevel.current == "high"

    val scaffoldBg = if (isDarkTheme) {
        if (isHighContrast) Color.Black else CupertinoDarkBg
    } else {
        if (isHighContrast) Color.White else CupertinoLightBg
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = scaffoldBg,
        topBar = {
            CupertinoNavigationBar(
                title = "About",
                leading = {
                    IconButton(
                        onClick = {
                            haptics.selection()
                            onNavigateBack()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (isDarkTheme) Color.White else Color.Black
                        )
                    }
                },
                textColor = if (isDarkTheme) Color.White else Color.Black
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // App Icon 96x96
            ColAILogoIcon(size = 96.dp)

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "ColAI",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    letterSpacing = (-0.5).sp
                ),
                color = if (isDarkTheme) Color.White else Color.Black
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Version ${BuildConfig.VERSION_NAME} (Native Kotlin Edition)",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = CupertinoSystemGrey
            )

            Spacer(modifier = Modifier.height(28.dp))

            // SECTION 1: ABOUT COLAI (General Public)
            CupertinoFormSection(
                header = "ABOUT COLAI",
                footer = "ColAI gives you complete privacy and freedom to run multiple accounts on all major AI platforms simultaneously.",
                isDark = isDarkTheme
            ) {
                CupertinoFormRow(
                    prefix = {
                        Text(
                            text = "Multi-Account",
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                            color = if (isDarkTheme) Color.White else Color.Black
                        )
                    },
                    isDark = isDarkTheme
                ) {
                    Text(
                        text = "Isolated Workspaces",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = CupertinoActiveBlue
                    )
                }

                CupertinoFormRow(
                    prefix = {
                        Text(
                            text = "Privacy",
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                            color = if (isDarkTheme) Color.White else Color.Black
                        )
                    },
                    isDark = isDarkTheme
                ) {
                    Text(
                        text = "100% On-Device & PIN Protected",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = CupertinoSystemGrey
                    )
                }

                CupertinoFormRow(
                    prefix = {
                        Text(
                            text = "Ad-Free",
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                            color = if (isDarkTheme) Color.White else Color.Black
                        )
                    },
                    showDivider = false,
                    isDark = isDarkTheme
                ) {
                    Text(
                        text = "Zero Ads, Zero Trackers",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = CupertinoSystemGrey
                    )
                }
            }

            // SECTION 2: DEVELOPER & SUPPORT
            CupertinoFormSection(
                header = "DEVELOPER & SUPPORT",
                isDark = isDarkTheme
            ) {
                CupertinoFormRow(
                    prefix = {
                        Text(
                            text = "Creator",
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                            color = if (isDarkTheme) Color.White else Color.Black
                        )
                    },
                    isDark = isDarkTheme
                ) {
                    Text(
                        text = "Ujwal",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = if (isDarkTheme) Color.White else Color.Black
                    )
                }

                CupertinoFormRow(
                    prefix = {
                        Text(
                            text = "Support & Donate",
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                            color = if (isDarkTheme) Color.White else Color.Black
                        )
                    },
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://buymemomo.com/ujwal"))
                        context.startActivity(intent)
                    },
                    isDark = isDarkTheme
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "buymemomo.com/ujwal",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            color = CupertinoActiveBlue
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = CupertinoActiveBlue,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                CupertinoFormRow(
                    prefix = {
                        Text(
                            text = "Source Code",
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                            color = if (isDarkTheme) Color.White else Color.Black
                        )
                    },
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Ujwal223/ColAI"))
                        context.startActivity(intent)
                    },
                    showDivider = false,
                    isDark = isDarkTheme
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "GitHub",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                            color = CupertinoActiveBlue
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = CupertinoActiveBlue,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            // SECTION 3: LEGAL
            CupertinoFormSection(
                header = "LEGAL",
                footer = "ColAI is free and open-source software under the Apache License 2.0.",
                isDark = isDarkTheme
            ) {
                CupertinoFormRow(
                    prefix = {
                        Text(
                            text = "License",
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                            color = if (isDarkTheme) Color.White else Color.Black
                        )
                    },
                    showDivider = false,
                    isDark = isDarkTheme
                ) {
                    Text(
                        text = "Apache 2.0",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                        color = CupertinoSystemGrey
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp).navigationBarsPadding())
        }
    }
}

@Composable
fun ColAILogoIcon(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 104.dp
) {
    val context = LocalContext.current
    val bitmap = remember {
        try {
            context.assets.open("images/colai_logo.png").use { inputStream ->
                android.graphics.BitmapFactory.decodeStream(inputStream)
            }
        } catch (_: Exception) {
            try {
                android.graphics.BitmapFactory.decodeResource(context.resources, R.drawable.colai_logo)
            } catch (_: Exception) {
                null
            }
        }
    }

    val cornerRadius = size * 0.22f
    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(cornerRadius),
                spotColor = Color.Black.copy(alpha = 0.30f)
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color(0xFF151517)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "ColAI Logo",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
        } else {
            Image(
                painter = painterResource(id = R.mipmap.launcher_icon),
                contentDescription = "ColAI Logo",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            )
        }
    }
}

