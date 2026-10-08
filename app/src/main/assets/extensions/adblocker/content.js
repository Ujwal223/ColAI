// ColAI Shield - Content Script Annoyance & Banner Suppressor
(function() {
  'use strict';

  // NEVER run on authentication, login, or OAuth pages
  const href = (window.location.href || '').toLowerCase();
  const host = (window.location.hostname || '').toLowerCase();
  if (
    href.includes('/login') ||
    href.includes('/auth') ||
    href.includes('/signin') ||
    href.includes('/oauth') ||
    href.includes('/callback') ||
    host.includes('accounts.google') ||
    host.includes('appleid.apple') ||
    host.includes('login.live') ||
    host.includes('login.microsoftonline') ||
    host.includes('auth.anthropic')
  ) {
    return;
  }

  const NAG_PHRASES = [
    'view in the grok app',
    'open grok',
    'get the claude app',
    'get the app',
    'use the app',
    'download the app',
    'install the app',
    'open in app',
    'continue in app',
    'download app',
    'install app'
  ];

  function autoDismissCookies() {
    const buttons = document.querySelectorAll(
      'button, div[role="button"], [data-testid="confirmationSheetConfirm"], #onetrust-accept-btn-handler'
    );
    const acceptPhrases = [
      'accept all cookies',
      'accept all',
      'refuse non-essential cookies',
      'accept cookies',
      'allow all cookies'
    ];
    for (let i = 0; i < buttons.length; i++) {
      const btn = buttons[i];
      const txt = (btn.innerText || btn.textContent || '').trim().toLowerCase();
      if (txt.length > 0 && txt.length < 30) {
        if (acceptPhrases.some(phrase => txt === phrase)) {
          try { btn.click(); } catch (_) {}
        }
      }
    }
  }

  function hideAppNags() {
    autoDismissCookies();

    // 1. Target known cookie consent dialogs & standalone download app links
    const knownSheets = document.querySelectorAll(
      "#onetrust-consent-sdk, .osano-cm-window, a[href*='claude.ai/download'], a[href*='play.google.com/store/apps/details?id=com.anthropic.claude']"
    );
    for (let k = 0; k < knownSheets.length; k++) {
      const el = knownSheets[k];
      el.style.setProperty('display', 'none', 'important');
      el.style.setProperty('height', '0px', 'important');
      el.style.setProperty('visibility', 'hidden', 'important');
    }

    // 2. Target explicit small banner elements ONLY (never climb parent tree!)
    const banners = document.querySelectorAll('div[role="banner"], aside[class*="banner" i], div[class*="promo" i]');
    for (let i = 0; i < banners.length; i++) {
      const el = banners[i];
      // Do not hide main layout or containers with chat input
      if (el.querySelector('#prompt-textarea, textarea, input, [contenteditable="true"], [role="textbox"], [role="dialog"]')) {
        continue;
      }
      // Safety: banner must not be a huge layout container
      if (el.offsetHeight > 200 || el.scrollHeight > 250) {
        continue;
      }
      const txt = (el.innerText || el.textContent || '').trim().toLowerCase();
      if (txt.length > 0 && txt.length < 100) {
        if (NAG_PHRASES.some(phrase => txt.includes(phrase))) {
          el.style.setProperty('display', 'none', 'important');
          el.style.setProperty('height', '0px', 'important');
          el.style.setProperty('visibility', 'hidden', 'important');
        }
      }
    }
  }

  // Run at start and observe changes
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', hideAppNags);
  } else {
    hideAppNags();
  }

  // Periodic sweeps during initial SPA hydration
  [300, 1000, 2500].forEach(delay => setTimeout(hideAppNags, delay));

  if (window.MutationObserver) {
    let debounceTimer = null;
    const observer = new MutationObserver(function() {
      if (debounceTimer) clearTimeout(debounceTimer);
      debounceTimer = setTimeout(hideAppNags, 200);
    });
    observer.observe(document.documentElement, {
      childList: true,
      subtree: true
    });
  }
})();
