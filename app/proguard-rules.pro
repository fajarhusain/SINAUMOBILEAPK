# ExamLock ProGuard Rules

# Keep all activity/service/receiver classes
-keep class com.examlock.browser.** { *; }

# WebView
-keepclassmembers class * extends android.webkit.WebViewClient {
    public void *(android.webkit.WebView, java.lang.String, android.graphics.Bitmap);
    public boolean *(android.webkit.WebView, java.lang.String);
}
-keepclassmembers class * extends android.webkit.WebChromeClient {
    public void *(android.webkit.WebView, java.lang.String);
}

# ML Kit
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.** { *; }

# ZXing
-keep class com.google.zxing.** { *; }

# CameraX
-keep class androidx.camera.** { *; }

# Keep JSON parsing
-keep class org.json.** { *; }

# General Android
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes EnclosingMethod
