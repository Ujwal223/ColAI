package com.ujwal.colai.core.ui.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ujwal.colai.core.model.AIService
import com.ujwal.colai.core.model.Session
import com.ujwal.colai.core.security.EncryptedStorage
import com.ujwal.colai.feature.home.ServiceCardIcon
import com.ujwal.colai.util.rememberHaptics

/**
 * Encapsulates incoming shared payload (text or media attachments) from Android share intent.
 */
data class SharedPayload(
    val text: String? = null,
    val uris: List<Uri> = emptyList()
)

/**
 * Cupertino-styled bottom sheet allowing users to route shared text or media
 * straight into their chosen AI service container and account session.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CupertinoShareTargetSheet(
    payload: SharedPayload,
    services: List<AIService>,
    sessions: List<Session>,
    onDismissRequest: () -> Unit,
    onTargetSelected: (service: AIService, session: Session) -> Unit,
    isDark: Boolean = true
) {
    val context = LocalContext.current
    val storage = remember { EncryptedStorage.getInstance(context) }
    val haptics = rememberHaptics()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var sessionToUnlock by remember { mutableStateOf<Pair<AIService, Session>?>(null) }

    val bgColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
    val cardBg = if (isDark) Color(0xFF2C2C2E) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val subTextColor = if (isDark) Color(0xFF8E8E93) else Color(0xFF6C6C70)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = bgColor,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 18.dp, bottom = 12.dp)
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Share to ColAI",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = textColor
                    )
                    Text(
                        text = "Choose an account to send content to",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        color = subTextColor
                    )
                }

                IconButton(
                    onClick = {
                        haptics.selection()
                        onDismissRequest()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = subTextColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Shared payload preview card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(cardBg)
                    .padding(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CupertinoActiveBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (!payload.text.isNullOrBlank()) Icons.Default.TextFields else Icons.Default.Image,
                            contentDescription = null,
                            tint = CupertinoActiveBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        if (!payload.text.isNullOrBlank()) {
                            Text(
                                text = payload.text,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                color = textColor,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (payload.uris.isNotEmpty()) {
                            Text(
                                text = "${payload.uris.size} media file(s) attached",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = CupertinoActiveBlue
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Grouped Accounts List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((320).dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(services) { service ->
                    val serviceSessions = sessions.filter { it.serviceId == service.id }
                    if (serviceSessions.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(cardBg)
                                .padding(vertical = 4.dp)
                        ) {
                            // Section header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(RoundedCornerShape(6.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    ServiceCardIcon(service = service, isDark = isDark)
                                }
                                Text(
                                    text = service.name.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = subTextColor
                                )
                            }

                            // Session rows
                            serviceSessions.forEachIndexed { index, session ->
                                val isLocked = storage.isSessionPinLocked(session.id)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            haptics.impactLight()
                                            if (isLocked) {
                                                sessionToUnlock = Pair(service, session)
                                            } else {
                                                onTargetSelected(service, session)
                                            }
                                        }
                                        .padding(horizontal = 14.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = session.accountName,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 15.sp
                                            ),
                                            color = textColor
                                        )

                                        if (session.isDefault) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = "Default",
                                                tint = Color(0xFFFFCC00),
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }

                                        if (isLocked) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = "Locked",
                                                tint = CupertinoActiveBlue,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = "Send",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        ),
                                        color = CupertinoActiveBlue
                                    )
                                }

                                if (index < serviceSessions.size - 1) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 14.dp)
                                            .height(0.5.dp)
                                            .background(if (isDark) Color(0x1FFFFFFF) else Color(0x1F000000))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // PIN Unlock Dialog if session is protected
    sessionToUnlock?.let { (service, session) ->
        CupertinoPinLockDialog(
            sessionName = session.accountName,
            onDismissRequest = { sessionToUnlock = null },
            onVerifyPin = { pin -> storage.verifySessionPin(session.id, pin) },
            onSuccess = {
                sessionToUnlock = null
                onTargetSelected(service, session)
            },
            isDark = isDark
        )
    }
}
