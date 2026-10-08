package com.ujwal.colai.feature.webview

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView

private const val TAG = "GeckoViewContainer"

/**
 * A lifecycle-aware, high-performance Jetpack Compose container for [GeckoView].
 *
 * Provides seamless integration with Mozilla GeckoView:
 * - Safely attaches and detaches [GeckoSession] instances.
 * - Handles tab switching by releasing previous sessions before attaching new ones.
 * - Observes Android Lifecycle events ([Lifecycle.Event.ON_RESUME], [Lifecycle.Event.ON_PAUSE], [Lifecycle.Event.ON_DESTROY]).
 * - Covers view with theme background until first paint to prevent white flash artifacts.
 *
 * @param session The active [GeckoSession] to render, or null if no session is active.
 * @param modifier Compose layout modifiers.
 * @param coverColor Optional color used to paint the view until first web frame renders.
 * @param onViewCreated Callback invoked once the native [GeckoView] instance is constructed.
 */
@Composable
fun GeckoViewContainer(
    session: GeckoSession?,
    modifier: Modifier = Modifier,
    coverColor: Color = MaterialTheme.colorScheme.background,
    onViewCreated: ((GeckoView) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Hold reference to the created GeckoView
    val geckoViewRef = remember { GeckoViewRef() }

    // Lifecycle observer to notify session when activity pauses/resumes
    DisposableEffect(lifecycleOwner, session) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    session?.let {
                        if (it.isOpen) {
                            it.setActive(true)
                            it.setPriorityHint(GeckoSession.PRIORITY_HIGH)
                        }
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    session?.let {
                        if (it.isOpen) {
                            it.setActive(false)
                            it.setPriorityHint(GeckoSession.PRIORITY_DEFAULT)
                        }
                    }
                }
                Lifecycle.Event.ON_DESTROY -> {
                    geckoViewRef.view?.let { view ->
                        safeReleaseSession(view)
                    }
                }
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Dispose effect when session changes or container leaves composition
    DisposableEffect(session) {
        onDispose {
            geckoViewRef.view?.let { view ->
                if (view.session == session) {
                    safeReleaseSession(view)
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(coverColor)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                createGeckoView(ctx, coverColor).also { view ->
                    geckoViewRef.view = view
                    onViewCreated?.invoke(view)
                    if (session != null) {
                        safeAttachSession(view, session)
                    }
                }
            },
            update = { view ->
                geckoViewRef.view = view
                val currentAttached = view.session

                if (currentAttached != session) {
                    Log.d(TAG, "Updating GeckoView session: from $currentAttached to $session")
                    if (currentAttached != null) {
                        safeReleaseSession(view)
                    }
                    if (session != null) {
                        safeAttachSession(view, session)
                    }
                }
            },
            onReset = { view ->
                safeReleaseSession(view)
            }
        )
    }
}

/**
 * Creates and pre-configures a [GeckoView] instance.
 */
private fun createGeckoView(context: Context, coverColor: Color): GeckoView {
    return GeckoView(context).apply {
        // Prevent jarring white flash before first paint
        coverUntilFirstPaint(coverColor.toArgb())
    }
}

/**
 * Safely attaches [session] to [view], catching any edge-case runtime exceptions.
 */
private fun safeAttachSession(view: GeckoView, session: GeckoSession) {
    try {
        if (view.session != session) {
            view.setSession(session)
            if (session.isOpen) {
                session.setActive(true)
                session.setPriorityHint(GeckoSession.PRIORITY_HIGH)
            }
            Log.d(TAG, "Successfully attached session to GeckoView")
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to attach GeckoSession to GeckoView", e)
    }
}

/**
 * Safely detaches any currently attached session from [view].
 */
private fun safeReleaseSession(view: GeckoView) {
    try {
        if (view.session != null) {
            view.releaseSession()
            Log.d(TAG, "Successfully released session from GeckoView")
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to release session from GeckoView", e)
    }
}

/**
 * Mutable container to hold GeckoView reference across recompositions.
 */
private class GeckoViewRef {
    var view: GeckoView? = null
}
