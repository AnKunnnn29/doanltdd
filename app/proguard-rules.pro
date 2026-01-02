# ==================== SECURITY RULES ====================
# Obfuscate all code to prevent reverse engineering
-repackageclasses ''
-allowaccessmodification
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*

# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ==================== ANTI-TAMPERING ====================
# Keep SecurityChecker class for runtime checks
-keep class com.example.doan.Utils.SecurityChecker { *; }
-keep class com.example.doan.Utils.SecurityCheckResult { *; }

# ==================== KEEP MODELS (Gson/Retrofit) ====================
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.example.doan.Models.** { *; }
-keep class com.example.doan.Network.** { *; }

# Retrofit
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keepclassmembernames interface * {
    @retrofit2.http.* <methods>;
}

# Gson
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ==================== ENCRYPTED SHARED PREFERENCES ====================
# Keep Tink crypto library (used by EncryptedSharedPreferences)
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# Keep AndroidX Security
-keep class androidx.security.crypto.** { *; }
-keepclassmembers class androidx.security.crypto.** { *; }

# ==================== BIOMETRIC / KEYSTORE ====================
-keep class com.example.doan.Utils.KeyStoreManager { *; }
-keep class androidx.biometric.** { *; }

# ==================== KEEP PARCELABLE ====================
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# ==================== ONESIGNAL ====================
-keep class com.onesignal.** { *; }
-dontwarn com.onesignal.**

# ==================== STOMP WEBSOCKET ====================
-keep class ua.naiksoftware.stomp.** { *; }
-dontwarn ua.naiksoftware.stomp.**

# ==================== RXJAVA ====================
-dontwarn io.reactivex.**
-keep class io.reactivex.** { *; }

# ==================== GLIDE ====================
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { <init>(...); }
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder { *** rewind(); }

# ==================== LOTTIE ====================
-dontwarn com.airbnb.lottie.**
-keep class com.airbnb.lottie.** { *; }

# ==================== GOOGLE MAPS ====================
-keep class com.google.android.gms.maps.** { *; }
-keep class com.google.android.libraries.places.** { *; }

# ==================== REMOVE LOGGING IN PRODUCTION ====================
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
    public static *** wtf(...);
}

# ==================== SECURITY: HIDE SENSITIVE METHODS ====================
# Remove sensitive method calls in production
-assumenosideeffects class * {
    public void setToken(...);
    public void setPassword(...);
    public void setApiKey(...);
    public void setSecretKey(...);
    public void setRefreshToken(...);
}

# ==================== PREVENT REFLECTION ATTACKS ====================
# Obfuscate class names aggressively
-flattenpackagehierarchy
-mergeinterfacesaggressively

# ==================== NATIVE METHODS ====================
-keepclasseswithmembernames class * {
    native <methods>;
}

# ==================== ENUMS ====================
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}