package com.ujwal.colai.feature.home

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ujwal.colai.core.data.DefaultServicesRepository
import com.ujwal.colai.core.model.AIService
import com.ujwal.colai.core.model.Session
import com.ujwal.colai.core.ui.components.ActionSheetAction
import com.ujwal.colai.core.ui.components.CupertinoActionSheetDialog
import com.ujwal.colai.core.ui.components.CupertinoActiveBlue
import com.ujwal.colai.core.ui.components.CupertinoActivityIndicator
import com.ujwal.colai.core.ui.components.CupertinoDarkBg
import com.ujwal.colai.core.ui.components.CupertinoDestructiveRed
import com.ujwal.colai.core.ui.components.CupertinoLightBg
import com.ujwal.colai.util.rememberHaptics

/**
 * EXACT iOS Cupertino HomeScreen:
 * - Cupertino large title navigation bar: "ColAI"
 * - 3-column grid (4 on wide tablets > 600dp) with childAspectRatio 0.72
 * - AIServiceCard items with rounded Liquid Glass logo container & session counts
 * - Floating Liquid Glass bottom navigation pill:
 *   - Height 70dp, corner radius 35dp
 *   - Theme Toggle button (Moon / Sun)
 *   - 50x50 Circle Add Button (+ in active blue)
 *   - Settings Button (Gear)
 * - Empty state with circular glass container
 * - Cupertino Action Sheet for service long-press ("Refresh Favicon", "Delete Service", "Cancel")
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onLaunchService: (AIService, Session?) -> Unit,
    onOpenManageSessions: (AIService) -> Unit,
    onOpenAddService: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleTheme: (() -> Unit)? = null,
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptics = rememberHaptics()
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp > 600

    var selectedServiceForOptions by remember { mutableStateOf<AIService?>(null) }

    val scaffoldBg = if (isDarkTheme) CupertinoDarkBg else CupertinoLightBg

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = scaffoldBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoading && uiState.services.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CupertinoActivityIndicator(
                        radius = 16.dp,
                        color = if (isDarkTheme) Color.White else Color.Black
                    )
                }
            } else if (uiState.services.isEmpty()) {
                // Empty State
                HomeEmptyState(
                    isDark = isDarkTheme,
                    onGetStarted = onOpenAddService
                )
            } else {
                // Exact 3-column Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(if (isTablet) 4 else 3),
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 120.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Cupertino Large Title
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = "ColAI",
                            style = androidx.compose.ui.text.TextStyle(
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-1.0).sp
                            ),
                            color = if (isDarkTheme) Color.White else Color.Black,
                            modifier = Modifier
                                .statusBarsPadding()
                                .padding(start = 4.dp, top = 8.dp, bottom = 8.dp)
                        )
                    }

                    items(
                        items = uiState.services,
                        key = { it.id }
                    ) { service ->
                        val sessions = uiState.sessionsMap[service.id] ?: emptyList()
                        val activeSession = sessions.find { it.isDefault } ?: sessions.firstOrNull()

                        AIServiceCard(
                            service = service,
                            sessionCount = sessions.size,
                            isDark = isDarkTheme,
                            onTap = {
                                viewModel.selectService(service, activeSession)
                                onLaunchService(service, activeSession)
                            },
                            onLongPress = {
                                selectedServiceForOptions = service
                            }
                        )
                    }
                }
            }

            // Floating Liquid Glass Bottom Navigation Bar (iOS 27 Gallery style)
            FloatingLiquidGlassNavBar(
                isDark = isDarkTheme,
                onThemeToggle = {
                    haptics.impactMedium()
                    onToggleTheme?.invoke()
                },
                onAddClick = {
                    haptics.impactLight()
                    onOpenAddService()
                },
                onSettingsClick = {
                    haptics.impactLight()
                    onOpenSettings()
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
            )

            // Cupertino Action Sheet on Service Long Press
            selectedServiceForOptions?.let { service ->
                val isDefault = service.id in listOf(
                    DefaultServicesRepository.ID_CHATGPT,
                    DefaultServicesRepository.ID_CLAUDE,
                    DefaultServicesRepository.ID_DEEPSEEK,
                    DefaultServicesRepository.ID_GROK,
                    DefaultServicesRepository.ID_GEMINI,
                    DefaultServicesRepository.ID_PERPLEXITY
                )

                val actions = mutableListOf<ActionSheetAction>()

                actions.add(
                    ActionSheetAction(
                        title = "Refresh Favicon",
                        onClick = {
                            viewModel.refreshFavicon(service)
                        }
                    )
                )

                if (!isDefault) {
                    actions.add(
                        ActionSheetAction(
                            title = "Delete Service",
                            isDestructive = true,
                            onClick = {
                                viewModel.deleteService(service)
                            }
                        )
                    )
                }

                CupertinoActionSheetDialog(
                    onDismissRequest = { selectedServiceForOptions = null },
                    title = service.name,
                    message = service.url,
                    actions = actions,
                    isDark = isDarkTheme
                )
            }
        }
    }
}

/**
 * Spring-animated button for iOS 27 Liquid Glass Navigation Bar.
 */
@Composable
fun LiquidGlassNavBarButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val currentOnClick by rememberUpdatedState(onClick)
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.86f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "ButtonSpringScale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(currentOnClick) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        try {
                            tryAwaitRelease()
                        } finally {
                            isPressed = false
                        }
                    },
                    onTap = {
                        currentOnClick()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * iOS 27 Gallery-style Floating Liquid Glass Navigation Bar:
 * - Centered floating capsule pill with authentic specular glint border
 * - Frosted translucent backdrop with rich dark/light mode balance
 * - Spring physics on tactile press
 * - Fluid Cupertino Active Blue center action button
 */
@Composable
fun FloatingLiquidGlassNavBar(
    isDark: Boolean,
    onThemeToggle: () -> Unit,
    onAddClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberHaptics()
    val isHighContrast = com.ujwal.colai.core.ui.theme.LocalContrastLevel.current == "high"
    val shape = CircleShape

    // Multi-layer frosted liquid glass
    val glassBg = if (isDark) {
        if (isHighContrast) Color(0xF2121214) else Color(0xDD1C1C20)
    } else {
        if (isHighContrast) Color(0xF8FFFFFF) else Color(0xEEF8F8FC)
    }

    val borderBrush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = if (isDark) 0.40f else 0.85f),
            (if (isDark) Color.White else Color.Black).copy(alpha = if (isDark) 0.12f else 0.08f),
            Color.Transparent
        )
    )

    val borderWidth = if (isHighContrast) 1.5.dp else 1.dp

    Box(
        modifier = modifier
            .shadow(
                elevation = if (isDark) 24.dp else 16.dp,
                shape = shape,
                spotColor = Color.Black.copy(alpha = if (isDark) 0.50f else 0.20f),
                ambientColor = Color.Black.copy(alpha = 0.25f)
            )
            .clip(shape)
            .background(glassBg)
            .border(width = borderWidth, brush = borderBrush, shape = shape)
            .padding(horizontal = 14.dp, vertical = 7.dp)
            .height(54.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Theme Toggle with tactile bounce
            LiquidGlassNavBarButton(
                onClick = {
                    haptics.impactLight()
                    onThemeToggle()
                },
                modifier = Modifier.size(44.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0x18FFFFFF) else Color(0x0C000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                        contentDescription = "Toggle Theme",
                        tint = if (isDark) Color.White else Color(0xFF1C1C1E),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Center: iOS 27 Fluid Cupertino Active Blue Action Button
            LiquidGlassNavBarButton(
                onClick = {
                    haptics.impactMedium()
                    onAddClick()
                },
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = CircleShape,
                            spotColor = CupertinoActiveBlue.copy(alpha = 0.5f)
                        )
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF2B8CFF),
                                    CupertinoActiveBlue
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.55f), Color.Transparent)
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Service",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            // Right: Settings Button
            LiquidGlassNavBarButton(
                onClick = {
                    haptics.impactLight()
                    onSettingsClick()
                },
                modifier = Modifier.size(44.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0x18FFFFFF) else Color(0x0C000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = if (isDark) Color.White else Color(0xFF1C1C1E),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

/**
 * EXACT Empty State:
 * - 120x120 glass container, borderRadius: 60, icon plus size 48
 * - "Your ColAI is Empty" (fontSize: 24, bold, -0.8sp)
 * - "Add your favorite AI services once and access them across all your accounts."
 * - "Get Started" filled button (borderRadius: 16dp)
 */
@Composable
private fun HomeEmptyState(
    isDark: Boolean,
    onGetStarted: () -> Unit
) {
    val haptics = rememberHaptics()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val glassBg = Brush.linearGradient(
            colors = listOf(
                (if (isDark) Color.White else Color.Black).copy(alpha = 0.10f),
                (if (isDark) Color.White else Color.Black).copy(alpha = 0.05f)
            )
        )
        val glassBorder = Brush.linearGradient(
            colors = listOf(
                (if (isDark) Color.White else Color.Black).copy(alpha = 0.50f),
                (if (isDark) Color.White else Color.Black).copy(alpha = 0.20f)
            )
        )

        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(glassBg)
                .border(width = 1.dp, brush = glassBorder, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = if (isDark) Color.White else Color.Black,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Your ColAI is Empty",
            style = androidx.compose.ui.text.TextStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.8).sp
            ),
            color = if (isDark) Color.White else Color.Black,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Add your favorite AI services once and access them across all your accounts.",
            style = androidx.compose.ui.text.TextStyle(
                fontSize = 15.sp,
                lineHeight = 21.sp
            ),
            color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.60f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = {
                haptics.impactLight()
                onGetStarted()
            },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CupertinoActiveBlue),
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 14.dp)
        ) {
            Text(
                text = "Get Started",
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = Color.White
            )
        }
    }
}
