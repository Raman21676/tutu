# ProGuard rules for Tutu Browser

# Keep DataStore
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

# Keep Compose
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions

# Remove logging in release but keep Log class for safety
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# Keep WebView
-keepclassmembers class androidx.webkit.** { *; }
-keep class androidx.webkit.** { *; }

# Keep WebScreen for debugging
-keep class com.tutu.browser.ui.screens.WebScreen { *; }
-keep class com.tutu.browser.ui.screens.WebScreen$* { *; }
