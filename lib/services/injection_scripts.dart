class InjectionScripts {
  static String triggerVoice(String url) {
    if (url.contains('chatgpt.com')) {
      return """
        (function() {
          const micButton = document.querySelector('button[aria-label="Voice mode"], button[aria-label="Speech output"]');
          if (micButton) micButton.click();
        })();
      """;
    } else if (url.contains('claude.ai')) {
      return """
        (function() {
          const micButton = document.querySelector('button[aria-label="Record audio"]');
          if (micButton) micButton.click();
        })();
      """;
    } else if (url.contains('gemini.google.com')) {
      return """
        (function() {
          const micButton = document.querySelector('button[aria-label="Use microphone"]');
          if (micButton) micButton.click();
        })();
      """;
    }
    return "";
  }

  static String focusSearch(String url) {
    return """
      (function() {
        const inputs = document.querySelectorAll('textarea, input[type="text"], [contenteditable="true"]');
        if (inputs.length > 0) {
          inputs[0].focus();
          setTimeout(() => inputs[0].focus(), 500);
        }
      })();
    """;
  }

  static String getThemeScript(bool isDarkMode) {
    if (isDarkMode) {
      return """
        (function() {
          document.documentElement.classList.add('dark');
          document.body.classList.add('dark');
          if (window.location.host.includes('chatgpt.com')) {
              document.documentElement.style.colorScheme = 'dark';
          }
        })();
      """;
    } else {
      return """
        (function() {
          document.documentElement.classList.remove('dark');
          document.body.classList.remove('dark');
          if (window.location.host.includes('chatgpt.com')) {
              document.documentElement.style.colorScheme = 'light';
          }
        })();
      """;
    }
  }

  static String getProviderSpecificCSS(String url) {
    if (url.contains('chatgpt.com')) {
      return """
        /* Hide potential debug text or empty list items */
        .text-token-text-secondary:empty { display: none !important; }
        /* Ensure top bar text isn't hidden by transparent/wrong color bg */
        .sticky.top-0 { background-color: inherit !important; }
      """;
    }
    return "";
  }
}
