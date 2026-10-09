# ==============================================================================
# ColAI Master ProGuard & R8 Configuration
# Target: Mozilla GeckoView, Jetpack Compose, Room DB, Jetpack Security Crypto
# ==============================================================================

-ignorewarnings

# ------------------------------------------------------------------------------
# Mozilla GeckoView Engine & JNI Bindings
# ------------------------------------------------------------------------------
-keep class org.mozilla.geckoview.** { *; }
-keep interface org.mozilla.geckoview.** { *; }
-keep class org.mozilla.gecko.** { *; }
-keep interface org.mozilla.gecko.** { *; }
-dontwarn org.mozilla.geckoview.**
-dontwarn org.mozilla.gecko.**
-dontwarn com.google.android.gms.**

# Keep GeckoView native JNI methods
-keepclasseswithmembers class org.mozilla.geckoview.** {
    native <methods>;
}
-keepclasseswithmembers class org.mozilla.gecko.** {
    native <methods>;
}

# ------------------------------------------------------------------------------
# AndroidX Room Database & SQLite
# ------------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class * extends androidx.room.migration.Migration { *; }
-dontwarn androidx.room.paging.**

# ------------------------------------------------------------------------------
# AndroidX Security Crypto & MasterKey
# ------------------------------------------------------------------------------
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# ------------------------------------------------------------------------------
# Jetpack Compose & Lifecycle
# ------------------------------------------------------------------------------
-dontwarn androidx.compose.**
-keepclassmembers class androidx.compose.ui.platform.AndroidComposeView {
    *;
}

# ------------------------------------------------------------------------------
# Kotlin Coroutines
# ------------------------------------------------------------------------------
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler { *; }
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ------------------------------------------------------------------------------
# ColAI Application Core, Models & Feature Providers
# ------------------------------------------------------------------------------
-keep class com.ujwal.colai.ColAIApp { *; }
-keep class com.ujwal.colai.MainActivity { *; }
-keep class com.ujwal.colai.core.model.** { *; }
-keep class com.ujwal.colai.core.security.** { *; }
-keepclassmembers class com.ujwal.colai.core.security.** { *; }
-keep class com.ujwal.colai.core.database.entity.** { *; }
-keep class com.ujwal.colai.core.database.dao.** { *; }
-keep class com.ujwal.colai.feature.widget.** { *; }
-keep class com.ujwal.colai.ServiceWidgetProvider { *; }
-keep class com.ujwal.colai.ServiceWidgetMediumProvider { *; }

# Keep native methods across ColAI
-keepclasseswithmembers class * {
    native <methods>;
}

# Keep Parcelable and Serializable implementations
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    !private <fields>;
    !private <methods>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ------------------------------------------------------------------------------
# General Optimizations & Line Number Preservation
# ------------------------------------------------------------------------------
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,InnerClasses,EnclosingMethod
-renamesourcefileattribute SourceFile

# Remove debug logs in production release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
