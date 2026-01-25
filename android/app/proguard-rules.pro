# ColAI ProGuard Rules
# ======================
# These rules protect the app's code and sensitive logic from reverse engineering
# while keeping necessary classes accessible for runtime reflection.

# Suppress warnings from third-party libraries
-ignorewarnings
-dontwarn com.google.android.play.core.**
-dontwarn com.google.android.gms.internal.**

# Keep Google Play Core (for in-app updates, reviews)
-keep class com.google.android.play.core.** { *; }

# Keep Flutter engine and plugins
-keep class io.flutter.app.** { *; }
-keep class io.flutter.plugin.** { *; }
-keep class io.flutter.util.** { *; }
-keep class io.flutter.view.** { *; }
-keep class io.flutter.embedding.** { *; }

# Keep SharedPreferences plugin (used for StorageService)
-keep class io.flutter.plugins.sharedpreferences.** { *; }

# Keep flutter_secure_storage (critical for encryption)
-keep class com.it_nomads.fluttersecurestorage.** { *; }

# Keep WebView related classes (for InAppWebView)
-keepclassmembers class fqcn.of.javascript.interface.for.webview {
   public *;
}
-keepclassmembers class * extends android.webkit.WebViewClient {
    public void *(android.webkit.WebView, java.lang.String, android.graphics.Bitmap);
    public boolean *(android.webkit.WebView, java.lang.String);
}
-keepclassmembers class * extends android.webkit.WebChromeClient {
    public void *(android.webkit.WebView, java.lang.String);
}

# Keep native Android widget providers
-keep class com.ujwal.colai.ServiceWidgetProvider { *; }
-keep class com.ujwal.colai.ServiceWidgetMediumProvider { *; }
-keep class com.ujwal.colai.MainActivity { *; }

# Aggressive obfuscation for everything else
# This makes reverse engineering significantly harder
-repackageclasses ''
-allowaccessmodification
-optimizationpasses 5

# Optimize and shrink code
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*

# Remove logging in release builds (important for security)
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# Keep line numbers for crash reports (helps with debugging production issues)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
