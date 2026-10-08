package com.ujwal.colai.feature.webview

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoSession

private const val TAG = "GeckoContentBlocker"

/**
 * Statistics snapshot for the network privacy shield.
 */
data class ShieldStats(
    val totalBlocked: Int = 0,
    val adsBlocked: Int = 0,
    val analyticsBlocked: Int = 0,
    val socialTrackersBlocked: Int = 0,
    val fingerprintingBlocked: Int = 0,
    val cryptominersBlocked: Int = 0,
    val isShieldEnabled: Boolean = true,
    val lastBlockedUri: String? = null
)

/**
 * Enterprise-grade Content Blocking and Network Privacy Shield.
 *
 * Implements [ContentBlocking.Delegate] to:
 * - Block invasive third-party ad networks, analytics beacons, and tracking scripts.
 * - Suppress intrusive "Download the Mobile App" nag banners and smart app promotion banners.
 * - Monitor and record real-time blocked threat statistics.
 */
class GeckoContentBlocker(
    initialShieldEnabled: Boolean = true
) : ContentBlocking.Delegate {

    private val _shieldStats = MutableStateFlow(
        ShieldStats(isShieldEnabled = initialShieldEnabled)
    )
    val shieldStats: StateFlow<ShieldStats> = _shieldStats.asStateFlow()

    override fun onContentBlocked(
        session: GeckoSession,
        event: ContentBlocking.BlockEvent
    ) {
        val category = event.antiTrackingCategory
        val uri = event.uri

        Log.d(TAG, "Blocked tracking resource: uri=$uri, category=$category")

        _shieldStats.update { current ->
            var ads = current.adsBlocked
            var analytics = current.analyticsBlocked
            var social = current.socialTrackersBlocked
            var fingerprinting = current.fingerprintingBlocked
            var crypto = current.cryptominersBlocked

            when (category) {
                ContentBlocking.AntiTracking.AD -> ads++
                ContentBlocking.AntiTracking.ANALYTIC -> analytics++
                ContentBlocking.AntiTracking.SOCIAL -> social++
                ContentBlocking.AntiTracking.FINGERPRINTING -> fingerprinting++
                ContentBlocking.AntiTracking.CRYPTOMINING -> crypto++
                else -> Unit
            }

            current.copy(
                totalBlocked = current.totalBlocked + 1,
                adsBlocked = ads,
                analyticsBlocked = analytics,
                socialTrackersBlocked = social,
                fingerprintingBlocked = fingerprinting,
                cryptominersBlocked = crypto,
                lastBlockedUri = uri
            )
        }
    }

    override fun onContentLoaded(
        session: GeckoSession,
        event: ContentBlocking.BlockEvent
    ) {
        // Logged for telemetry/diagnostic purposes if needed
    }

    /**
     * Attaches this content blocker delegate to [session].
     */
    fun attach(session: GeckoSession) {
        session.contentBlockingDelegate = this
        Log.d(TAG, "Content blocking shield attached to GeckoSession")
    }

    /**
     * Detaches this content blocker delegate from [session].
     */
    fun detach(session: GeckoSession) {
        if (session.contentBlockingDelegate == this) {
            session.contentBlockingDelegate = null
            Log.d(TAG, "Content blocking shield detached from GeckoSession")
        }
    }

    /**
     * Resets blocked counters (e.g. on new session or page navigation).
     */
    fun resetStats() {
        _shieldStats.update {
            it.copy(
                totalBlocked = 0,
                adsBlocked = 0,
                analyticsBlocked = 0,
                socialTrackersBlocked = 0,
                fingerprintingBlocked = 0,
                cryptominersBlocked = 0,
                lastBlockedUri = null
            )
        }
    }

    /**
     * Toggles network shield state.
     */
    fun setShieldEnabled(enabled: Boolean) {
        _shieldStats.update { it.copy(isShieldEnabled = enabled) }
    }

    companion object {
        /**
         * CSS selectors identifying mobile app download banners, smart banners,
         * cookie consent notices, and native app upsells across ChatGPT, Claude, Grok, etc.
         */
        val ANTI_APP_BANNER_SELECTORS: List<String> = listOf(
            "a[href*='play.google.com/store/apps']",
            "a[href*='apps.apple.com']",
            "a[href*='claude.ai/download']",
            ".mobile-app-promo",
            "[data-testid*='mobile-banner']",
            "[data-testid*='download-banner']",
            "[class*='DownloadAppBanner']",
            "[class*='MobileBanner']",
            "#onetrust-consent-sdk",
            "#onetrust-banner-sdk",
            ".osano-cm-window",
            ".cc-banner"
        )

        /**
         * Generates CSS stylesheet rules suppressing annoying mobile download banners.
         */
        fun getAntiAppBannerCss(): String {
            val joined = ANTI_APP_BANNER_SELECTORS.joinToString(",\n")
            return "$joined {\n  display: none !important;\n  visibility: hidden !important;\n  height: 0 !important;\n  max-height: 0 !important;\n  opacity: 0 !important;\n  pointer-events: none !important;\n}"
        }

        /**
         * Actively injects stylesheet rules and DOM observer suppressing install/download app banners.
         */
        fun injectAntiAppBanner(session: GeckoSession) {
            val cleanCss = getAntiAppBannerCss().replace("\n", " ").replace("\"", "\\\"")
            val js = """
                javascript:(function(){
                    try {
                        var h = (window.location.href || '').toLowerCase();
                        var host = (window.location.hostname || '').toLowerCase();
                        if (h.indexOf('/login') !== -1 || h.indexOf('/auth') !== -1 || h.indexOf('/signin') !== -1 || h.indexOf('/oauth') !== -1 || h.indexOf('/callback') !== -1 || host.indexOf('accounts.google') !== -1 || host.indexOf('anthropic.com') !== -1) {
                            return;
                        }
                        if (!document.getElementById('colai-anti-banner-style')) {
                            var s = document.createElement('style');
                            s.id = 'colai-anti-banner-style';
                            s.textContent = "$cleanCss";
                            (document.head || document.documentElement).appendChild(s);
                        }
                        var isSafe = function(el) {
                            if (!el || el === document.body || el === document.documentElement) return false;
                            var tag = (el.tagName || '').toLowerCase();
                            if (tag === 'body' || tag === 'html' || tag === 'main' || tag === 'form' || tag === 'article' || tag === 'header' || tag === 'nav') return false;
                            if (el.id === 'root' || el.id === '__next' || el.id === 'app' || el.id === 'grok-app') return false;
                            if (el.querySelector('#prompt-textarea, textarea, input, [contenteditable="true"], [role="textbox"], [role="dialog"]')) return false;
                            if (el.offsetHeight > 180 || el.scrollHeight > 220) return false;
                            return true;
                        };
                        var autoDismissCookies = function() {
                            var buttons = document.querySelectorAll('button, div[role="button"]');
                            var acceptPhrases = ['accept all cookies', 'accept all', 'refuse non-essential cookies', 'accept cookies', 'allow all cookies'];
                            for (var b = 0; b < buttons.length; b++) {
                                var btn = buttons[b];
                                var txt = (btn.innerText || btn.textContent || '').trim().toLowerCase();
                                if (txt.length > 0 && txt.length < 30) {
                                    if (acceptPhrases.some(function(p) { return txt === p; })) {
                                        try { btn.click(); } catch(_) {}
                                    }
                                }
                            }
                        };
                        var hideAppNags = function() {
                            autoDismissCookies();
                            var els = document.querySelectorAll("${ANTI_APP_BANNER_SELECTORS.joinToString(",")}");
                            for (var i = 0; i < els.length; i++) {
                                if (isSafe(els[i])) {
                                    els[i].style.setProperty('display', 'none', 'important');
                                    els[i].style.setProperty('height', '0', 'important');
                                    els[i].style.setProperty('visibility', 'hidden', 'important');
                                }
                            }
                            var textEls = document.querySelectorAll('div[role="banner"], aside[class*="banner" i], div[class*="promo" i]');
                            var nagPhrases = ['install app', 'get the app', 'download app', 'view in the grok app', 'get the claude app', 'download the app'];
                            for (var j = 0; j < textEls.length; j++) {
                                var el = textEls[j];
                                var txt = (el.innerText || el.textContent || '').trim().toLowerCase();
                                if (txt.length > 0 && txt.length < 90) {
                                    var matched = nagPhrases.some(function(phrase) { return txt.indexOf(phrase) !== -1; });
                                    if (matched && isSafe(el)) {
                                        el.style.setProperty('display', 'none', 'important');
                                        el.style.setProperty('height', '0', 'important');
                                        el.style.setProperty('visibility', 'hidden', 'important');
                                    }
                                }
                            }
                        };
                        hideAppNags();
                        [600, 1500].forEach(function(delay) { setTimeout(hideAppNags, delay); });
                    } catch(e) {}
                })();
            """.trimIndent().replace("\n", "")

            try {
                if (session.isOpen) {
                    session.loadUri(js)
                }
            } catch (e: Exception) {
                Log.d(TAG, "Error injecting anti-app-banner script", e)
            }
        }
    }
}
