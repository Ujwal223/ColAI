package com.ujwal.colai.feature.home

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ujwal.colai.core.database.AppDatabase
import com.ujwal.colai.core.model.AIService
import com.ujwal.colai.core.model.Session
import com.ujwal.colai.core.security.EncryptedStorage
import com.ujwal.colai.core.ui.components.ActionSheetAction
import com.ujwal.colai.core.ui.components.CupertinoActionSheetDialog
import com.ujwal.colai.core.ui.components.CupertinoActiveBlue
import com.ujwal.colai.core.ui.components.CupertinoActivityIndicator
import com.ujwal.colai.core.ui.components.CupertinoAlertDialog
import com.ujwal.colai.core.ui.components.CupertinoDarkBg
import com.ujwal.colai.core.ui.components.CupertinoFormRow
import com.ujwal.colai.core.ui.components.CupertinoFormSection
import com.ujwal.colai.core.ui.components.CupertinoLightBg
import com.ujwal.colai.core.ui.components.CupertinoNavigationBar
import com.ujwal.colai.core.ui.components.CupertinoSystemGrey
import com.ujwal.colai.core.ui.theme.LocalContrastLevel
import com.ujwal.colai.util.rememberHaptics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * Native Cupertino AddServiceScreen:
 * - Top Bar: "New Service", Leading: "Cancel", Trailing: "Done" (bold)
 * - 110x110 Interactive Logo Preview with Letter Avatar placeholder or edited artwork
 * - One-tap Logo Selection: Choose File/Photo OR Enter Image Link
 * - Integrated Logo Editor: Live preview, 90-degree rotation, square crop, and save
 * - Section "ESSENTIALS": Name (TextField), URL (TextField)
 */
@Composable
fun AddServiceScreen(
    onNavigateBack: () -> Unit,
    onServiceCreated: (AIService) -> Unit,
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }
    val storage = remember { EncryptedStorage.getInstance(context) }

    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var localImagePath by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var isFetchingLink by remember { mutableStateOf(false) }
    var showInvalidInputDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var showLogoSourceSheet by remember { mutableStateOf(false) }
    var showLinkInputDialog by remember { mutableStateOf(false) }
    var pendingBitmapToEdit by remember { mutableStateOf<Bitmap?>(null) }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val bitmap = context.contentResolver.openInputStream(uri)?.use {
                        BitmapFactory.decodeStream(it)
                    }
                    if (bitmap != null) {
                        withContext(Dispatchers.Main) {
                            pendingBitmapToEdit = bitmap
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    val isHighContrast = LocalContrastLevel.current == "high"
    val scaffoldBg = if (isDarkTheme) {
        if (isHighContrast) Color.Black else CupertinoDarkBg
    } else CupertinoLightBg

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = scaffoldBg,
        topBar = {
            CupertinoNavigationBar(
                title = "New Service",
                leading = {
                    TextButton(
                        onClick = {
                            haptics.selection()
                            onNavigateBack()
                        }
                    ) {
                        Text(
                            text = "Cancel",
                            color = CupertinoActiveBlue,
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp)
                        )
                    }
                },
                trailing = {
                    if (isSaving) {
                        CupertinoActivityIndicator(
                            radius = 9.dp,
                            color = CupertinoActiveBlue
                        )
                    } else {
                        TextButton(
                            onClick = {
                                val trimmedName = name.trim()
                                val trimmedUrl = url.trim()

                                if (trimmedName.isEmpty() || trimmedUrl.isEmpty()) {
                                    haptics.error()
                                    showInvalidInputDialog = true
                                    return@TextButton
                                }

                                isSaving = true
                                haptics.impactLight()

                                scope.launch(Dispatchers.IO) {
                                    val serviceId = "custom_${UUID.randomUUID().toString().take(8)}"
                                    val defaultSessionId = UUID.randomUUID().toString()

                                    val normalizedUrl = if (!trimmedUrl.startsWith("http://") && !trimmedUrl.startsWith("https://")) {
                                        "https://$trimmedUrl"
                                    } else {
                                        trimmedUrl
                                    }

                                    val service = AIService(
                                        id = serviceId,
                                        name = trimmedName,
                                        url = normalizedUrl,
                                        faviconUrl = "",
                                        iconPath = localImagePath,
                                        widgetSessionId = defaultSessionId,
                                        sortOrder = 99
                                    )

                                    val initialSession = Session(
                                        id = defaultSessionId,
                                        serviceId = serviceId,
                                        accountName = "Primary",
                                        isDefault = true,
                                        lastAccessed = System.currentTimeMillis()
                                    )

                                    db.serviceDao().insertService(service)
                                    db.sessionDao().insertSession(initialSession)
                                    storage.setActiveServiceId(serviceId)
                                    storage.setActiveSessionId(defaultSessionId)

                                    withContext(Dispatchers.Main) {
                                        haptics.success()
                                        onServiceCreated(service)
                                    }
                                }
                            }
                        ) {
                            Text(
                                text = "Done",
                                color = CupertinoActiveBlue,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                )
                            )
                        }
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
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon Preview Container (110x110)
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .shadow(
                        elevation = if (isDarkTheme) 20.dp else 4.dp,
                        shape = RoundedCornerShape(40.dp),
                        spotColor = Color.Black.copy(alpha = 0.30f)
                    )
                    .clip(RoundedCornerShape(40.dp))
                    .background(if (isDarkTheme) Color(0xFF151517) else Color(0xFFE5E5EA))
                    .clickable {
                        haptics.impactLight()
                        showLogoSourceSheet = true
                    },
                contentAlignment = Alignment.Center
            ) {
                if (localImagePath != null) {
                    val bitmap = remember(localImagePath) {
                        try {
                            BitmapFactory.decodeFile(localImagePath)
                        } catch (_: Exception) {
                            null
                        }
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Service Logo",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(18.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        LetterAvatar(name = name, isDark = isDarkTheme)
                    }
                } else {
                    LetterAvatar(name = name, isDark = isDarkTheme)
                }

                // Subtitle camera hint badge if no custom logo
                if (localImagePath == null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(CupertinoActiveBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddAPhoto,
                            contentDescription = "Add Logo",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Add Logo / Change Logo Button
            TextButton(
                onClick = {
                    haptics.selection()
                    showLogoSourceSheet = true
                }
            ) {
                Text(
                    text = if (localImagePath == null) "Add Logo" else "Change Logo",
                    color = CupertinoActiveBlue,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section ESSENTIALS
            CupertinoFormSection(
                header = "ESSENTIALS",
                isDark = isDarkTheme
            ) {
                // Name
                CupertinoFormRow(
                    prefix = {
                        Text(
                            text = "Name",
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                            color = if (isDarkTheme) Color.White else Color.Black
                        )
                    },
                    isDark = isDarkTheme
                ) {
                    BasicTextField(
                        value = name,
                        onValueChange = { name = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = if (isDarkTheme) Color.White else Color.Black,
                            fontSize = 16.sp,
                            textAlign = TextAlign.End
                        ),
                        cursorBrush = SolidColor(CupertinoActiveBlue),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.width(180.dp),
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterEnd) {
                                if (name.isEmpty()) {
                                    Text(
                                        text = "Required",
                                        style = TextStyle(fontSize = 16.sp, color = CupertinoSystemGrey),
                                        textAlign = TextAlign.End
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }

                // URL
                CupertinoFormRow(
                    prefix = {
                        Text(
                            text = "URL",
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                            color = if (isDarkTheme) Color.White else Color.Black
                        )
                    },
                    showDivider = false,
                    isDark = isDarkTheme
                ) {
                    BasicTextField(
                        value = url,
                        onValueChange = { url = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = if (isDarkTheme) Color.White else Color.Black,
                            fontSize = 16.sp,
                            textAlign = TextAlign.End
                        ),
                        cursorBrush = SolidColor(CupertinoActiveBlue),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                        modifier = Modifier.width(220.dp),
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterEnd) {
                                if (url.isEmpty()) {
                                    Text(
                                        text = "https://...",
                                        style = TextStyle(fontSize = 16.sp, color = CupertinoSystemGrey),
                                        textAlign = TextAlign.End
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp).navigationBarsPadding())
        }
    }

    // Action Sheet: Logo Sourcing Options
    if (showLogoSourceSheet) {
        val actions = mutableListOf<ActionSheetAction>()

        actions.add(
            ActionSheetAction(
                title = "Choose from Photos / Files",
                onClick = {
                    photoPickerLauncher.launch("image/*")
                }
            )
        )

        actions.add(
            ActionSheetAction(
                title = "Add an Image Link",
                onClick = {
                    showLinkInputDialog = true
                }
            )
        )

        if (localImagePath != null) {
            actions.add(
                ActionSheetAction(
                    title = "Remove Logo",
                    isDestructive = true,
                    onClick = {
                        localImagePath = null
                    }
                )
            )
        }

        CupertinoActionSheetDialog(
            onDismissRequest = { showLogoSourceSheet = false },
            title = "Service Logo",
            message = "Select a source for your AI service icon",
            actions = actions,
            isDark = isDarkTheme
        )
    }

    // Dialog: Enter Image Link
    if (showLinkInputDialog) {
        var linkUrl by remember { mutableStateOf("") }

        CupertinoInputDialog(
            title = "Enter Image Link",
            message = "Paste a direct web link to an image (PNG, JPG, WebP)",
            initialValue = linkUrl,
            placeholder = "https://example.com/logo.png",
            confirmTitle = "Fetch",
            isLoading = isFetchingLink,
            onConfirm = { inputUrl ->
                val trimmed = inputUrl.trim()
                if (trimmed.isBlank() || (!trimmed.startsWith("http://") && !trimmed.startsWith("https://"))) {
                    errorMessage = "Please enter a valid HTTP or HTTPS image URL."
                    return@CupertinoInputDialog
                }
                isFetchingLink = true
                scope.launch(Dispatchers.IO) {
                    val fetched = downloadBitmapFromUrl(trimmed)
                    withContext(Dispatchers.Main) {
                        isFetchingLink = false
                        if (fetched != null) {
                            showLinkInputDialog = false
                            pendingBitmapToEdit = fetched
                        } else {
                            errorMessage = "Unable to fetch image from link. Please check the URL and try again."
                        }
                    }
                }
            },
            onDismissRequest = { showLinkInputDialog = false },
            isDark = isDarkTheme
        )
    }

    // Interactive Logo Editor Modal Dialog (Crop & Rotate)
    pendingBitmapToEdit?.let { rawBitmap ->
        LogoEditorDialog(
            rawBitmap = rawBitmap,
            isDark = isDarkTheme,
            onDismiss = { pendingBitmapToEdit = null },
            onSave = { editedBitmap ->
                scope.launch(Dispatchers.IO) {
                    try {
                        val maxDim = maxOf(editedBitmap.width, editedBitmap.height)
                        val scaled = if (maxDim > 512) {
                            val scale = 512f / maxDim
                            Bitmap.createScaledBitmap(
                                editedBitmap,
                                (editedBitmap.width * scale).toInt().coerceAtLeast(1),
                                (editedBitmap.height * scale).toInt().coerceAtLeast(1),
                                true
                            )
                        } else editedBitmap

                        val targetFile = File(context.filesDir, "custom_${UUID.randomUUID()}.png")
                        val outputStream = FileOutputStream(targetFile)
                        scaled.compress(Bitmap.CompressFormat.PNG, 95, outputStream)
                        outputStream.flush()
                        outputStream.close()

                        withContext(Dispatchers.Main) {
                            localImagePath = targetFile.absolutePath
                            pendingBitmapToEdit = null
                            haptics.success()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            errorMessage = "Error saving logo: ${e.message}"
                            pendingBitmapToEdit = null
                        }
                    }
                }
            }
        )
    }

    // Invalid Input Alert Dialog
    if (showInvalidInputDialog) {
        CupertinoAlertDialog(
            onDismissRequest = { showInvalidInputDialog = false },
            title = "Invalid Input",
            message = "Please enter both name and URL.",
            confirmTitle = "OK",
            onConfirm = { showInvalidInputDialog = false },
            isDark = isDarkTheme
        )
    }

    // General Error Alert Dialog
    errorMessage?.let { error ->
        CupertinoAlertDialog(
            onDismissRequest = { errorMessage = null },
            title = "Notice",
            message = error,
            confirmTitle = "OK",
            onConfirm = { errorMessage = null },
            isDark = isDarkTheme
        )
    }
}

/**
 * Interactive Logo Editor Dialog providing live preview, 90-degree rotation, and square crop.
 */
@Composable
fun LogoEditorDialog(
    rawBitmap: Bitmap,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onSave: (Bitmap) -> Unit
) {
    val haptics = rememberHaptics()
    var rotationDegrees by remember { mutableIntStateOf(0) }
    var isSquareCrop by remember { mutableStateOf(true) }

    // Compute live edited preview bitmap
    val previewBitmap = remember(rawBitmap, rotationDegrees, isSquareCrop) {
        try {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            val rotated = Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
            if (isSquareCrop) {
                val minDim = minOf(rotated.width, rotated.height)
                val startX = (rotated.width - minDim) / 2
                val startY = (rotated.height - minDim) / 2
                Bitmap.createBitmap(rotated, startX, startY, minDim, minDim)
            } else {
                rotated
            }
        } catch (_: Exception) {
            rawBitmap
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val cardBg = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
        val textColor = if (isDark) Color.White else Color.Black

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(cardBg)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Edit Logo",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = textColor
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Crop and rotate your AI icon preview",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = CupertinoSystemGrey
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Preview Box
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(if (isDark) Color(0xFF151517) else Color(0xFFE5E5EA))
                        .border(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0x1F000000), RoundedCornerShape(32.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = previewBitmap.asImageBitmap(),
                        contentDescription = "Edited Preview",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Controls: Rotate & Crop Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Rotate Button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                haptics.impactLight()
                                rotationDegrees = (rotationDegrees + 90) % 360
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.RotateRight,
                            contentDescription = "Rotate 90°",
                            tint = CupertinoActiveBlue,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Rotate 90°",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = CupertinoActiveBlue
                        )
                    }

                    // Square Crop Toggle Button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                haptics.selection()
                                isSquareCrop = !isSquareCrop
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Crop,
                            contentDescription = "Square Crop",
                            tint = if (isSquareCrop) CupertinoActiveBlue else CupertinoSystemGrey,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isSquareCrop) "Square (1:1)" else "Original",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = if (isSquareCrop) CupertinoActiveBlue else CupertinoSystemGrey
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Bottom Buttons: Cancel & Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA))
                    ) {
                        Text(
                            text = "Cancel",
                            color = if (isDark) Color.White else Color.Black,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal)
                        )
                    }

                    TextButton(
                        onClick = {
                            haptics.impactMedium()
                            onSave(previewBitmap)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CupertinoActiveBlue)
                    ) {
                        Text(
                            text = "Save Logo",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Cupertino Text Input Dialog for pasting URLs.
 */
@Composable
fun CupertinoInputDialog(
    title: String,
    message: String,
    initialValue: String = "",
    placeholder: String = "",
    confirmTitle: String = "OK",
    cancelTitle: String = "Cancel",
    isLoading: Boolean = false,
    onConfirm: (String) -> Unit,
    onDismissRequest: () -> Unit,
    isDark: Boolean = true
) {
    var textValue by remember { mutableStateOf(initialValue) }
    val haptics = rememberHaptics()
    val dialogBg = if (isDark) Color(0xFF252525) else Color(0xFFF2F2F2)
    val inputBg = if (isDark) Color(0xFF1C1C1E) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val dividerColor = if (isDark) Color(0x2EFFFFFF) else Color(0x22000000)

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(dialogBg),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = textColor,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        color = CupertinoSystemGrey,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    BasicTextField(
                        value = textValue,
                        onValueChange = { textValue = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = textColor,
                            fontSize = 14.sp
                        ),
                        cursorBrush = SolidColor(CupertinoActiveBlue),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(inputBg)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        decorationBox = { innerTextField ->
                            Box {
                                if (textValue.isEmpty()) {
                                    Text(
                                        text = placeholder,
                                        style = TextStyle(fontSize = 14.sp, color = CupertinoSystemGrey)
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }

                androidx.compose.material3.HorizontalDivider(thickness = 0.5.dp, color = dividerColor)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    TextButton(
                        onClick = {
                            haptics.selection()
                            onDismissRequest()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                        shape = RoundedCornerShape(0.dp)
                    ) {
                        Text(
                            text = cancelTitle,
                            color = CupertinoActiveBlue,
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp)
                        )
                    }

                    androidx.compose.material3.VerticalDivider(thickness = 0.5.dp, color = dividerColor)

                    TextButton(
                        onClick = {
                            haptics.selection()
                            onConfirm(textValue)
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                        shape = RoundedCornerShape(0.dp)
                    ) {
                        if (isLoading) {
                            CupertinoActivityIndicator(radius = 8.dp, color = CupertinoActiveBlue)
                        } else {
                            Text(
                                text = confirmTitle,
                                color = CupertinoActiveBlue,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Downloads a bitmap from a public HTTP/HTTPS URL with safety limits and timeouts.
 */
private fun downloadBitmapFromUrl(urlString: String): Bitmap? {
    var connection: HttpURLConnection? = null
    return try {
        val url = URL(urlString)
        connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 6000
            readTimeout = 8000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile) ColAI/2.0")
            setRequestProperty("Accept", "image/*")
        }

        if (connection.responseCode in 200..299) {
            connection.inputStream.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } else null
    } catch (_: Exception) {
        null
    } finally {
        connection?.disconnect()
    }
}

@Composable
private fun LetterAvatar(name: String, isDark: Boolean) {
    val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Text(
        text = initial,
        style = TextStyle(
            fontSize = 44.sp,
            fontWeight = FontWeight.Light,
            color = if (isDark) Color(0xFF3A3A3C) else Color(0xFFC7C7CC)
        )
    )
}
