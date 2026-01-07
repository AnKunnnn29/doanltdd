# ==================== SECURITY RULES ====================
# Obfuscate all code to prevent reverse engineering
-repackageclasses ''
-allowaccessmodification
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*

# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ==================== APPLICATION CLASS ====================
-keep class com.example.doan.UTETeaApplication { *; }

# ==================== ANTI-TAMPERING ====================
# Keep SecurityChecker class for runtime checks
-keep class com.example.doan.Utils.SecurityChecker { *; }
-keep class com.example.doan.Utils.SecurityCheckResult { *; }

# ==================== KEEP ALL UTILS ====================
-keep class com.example.doan.Utils.** { *; }

# ==================== KEEP ACTIVITIES & FRAGMENTS ====================
-keep class com.example.doan.Activities.** { *; }
-keep class com.example.doan.Fragments.** { *; }

# ==================== KEEP ADAPTERS ====================
-keep class com.example.doan.Adapters.** { *; }

# ==================== KEEP SERVICES ====================
-keep class com.example.doan.Services.** { *; }

# ==================== KEEP VIEWS ====================
-keep class com.example.doan.Views.** { *; }

# ==================== KEEP UI (Compose) ====================
-keep class com.example.doan.ui.** { *; }

# ==================== KEEP MODELS (Gson/Retrofit) ====================
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes Exceptions
-keep class com.example.doan.Models.** { *; }
-keepclassmembers class com.example.doan.Models.** { *; }
-keep class com.example.doan.Network.** { *; }
-keepclassmembers class com.example.doan.Network.** { *; }

# Retrofit - CRITICAL for API calls
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations

-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keepclassmembernames interface * {
    @retrofit2.http.* <methods>;
}

# Keep generic signature of Call, Response (R8 full mode strips signatures from non-kept items)
-keep,allowobfuscation,allowshrinking class retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# Keep Retrofit service methods
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Keep ApiService interface - CRITICAL
-keep interface com.example.doan.Network.ApiService { *; }
-keep class com.example.doan.Network.RetrofitClient { *; }
-keep class com.example.doan.Network.AuthInterceptor { *; }

# Gson
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Gson specific classes - CRITICAL for generic types
-keepattributes Signature
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.stream.** { *; }

# Keep generic type information for Gson
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

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
-keep class org.json.** { *; }

# ==================== STOMP WEBSOCKET ====================
-keep class ua.naiksoftware.stomp.** { *; }
-dontwarn ua.naiksoftware.stomp.**

# ==================== RXJAVA ====================
-dontwarn io.reactivex.**
-keep class io.reactivex.** { *; }
-keepclassmembers class io.reactivex.** { *; }

# ==================== KOTLIN ====================
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Lazy {
    public <methods>;
}
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
}

# ==================== KOTLINX SERIALIZATION ====================
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# ==================== COROUTINES ====================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ==================== JETPACK COMPOSE ====================
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
-keep class androidx.activity.ComponentActivity { *; }

# ==================== COIL (Image Loading for Compose) ====================
-keep class coil.** { *; }
-dontwarn coil.**

# ==================== RIVE ANIMATION ====================
-keep class app.rive.** { *; }
-dontwarn app.rive.**

# ==================== MPANDROIDCHART ====================
-keep class com.github.mikephil.charting.** { *; }
-dontwarn com.github.mikephil.charting.**

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
-keep class com.google.android.gms.location.** { *; }
-dontwarn com.google.android.gms.**

# ==================== ANDROIDX ====================
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-dontwarn androidx.**

# ==================== LIFECYCLE ====================
-keep class androidx.lifecycle.** { *; }
-keepclassmembers class * implements androidx.lifecycle.LifecycleObserver {
    <init>(...);
}
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keepclassmembers class androidx.lifecycle.Lifecycle$State { *; }
-keepclassmembers class androidx.lifecycle.Lifecycle$Event { *; }
-keepclassmembers class * {
    @androidx.lifecycle.OnLifecycleEvent *;
}

# ==================== NAVIGATION ====================
-keep class androidx.navigation.** { *; }
-keepnames class androidx.navigation.fragment.NavHostFragment

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

# ==================== SERIALIZABLE ====================
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ==================== R8 FULL MODE ====================
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# ==================== SUPPRESS WARNINGS ====================
-dontwarn java.lang.invoke.StringConcatFactory
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ==================== APPLICATION CLASS ====================
-keep class com.example.doan.UTETeaApplication { *; }

# ==================== ACTIVITIES ====================
-keep class com.example.doan.Activities.** { *; }

# ==================== ADAPTERS ====================
-keep class com.example.doan.Adapters.** { *; }

# ==================== FRAGMENTS ====================
-keep class com.example.doan.Fragments.** { *; }

# ==================== SERVICES ====================
-keep class com.example.doan.Services.** { *; }

# ==================== VIEWS ====================
-keep class com.example.doan.Views.** { *; }

# ==================== RIVE ANIMATION ====================
-keep class app.rive.** { *; }
-dontwarn app.rive.**

# ==================== MPANDROIDCHART ====================
-keep class com.github.mikephil.charting.** { *; }
-dontwarn com.github.mikephil.charting.**

# ==================== COIL (Compose Image Loading) ====================
-keep class coil.** { *; }
-dontwarn coil.**

# ==================== NAVIGATION COMPONENT ====================
-keep class androidx.navigation.** { *; }
-keepnames class * extends android.os.Parcelable
-keepnames class * extends java.io.Serializable

# ==================== COMPOSE ====================
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ==================== CIRCLE INDICATOR ====================
-keep class me.relex.circleindicator.** { *; }

# ==================== SNAPPER ====================
-keep class dev.chrisbanes.snapper.** { *; }
-dontwarn dev.chrisbanes.snapper.**

# ==================== KOTLIN SERIALIZATION ====================
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ==================== KOTLIN COROUTINES ====================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ==================== LIFECYCLE ====================
-keep class androidx.lifecycle.** { *; }
-keepclassmembers class * implements androidx.lifecycle.LifecycleObserver {
    <init>(...);
}

# ==================== PREVENT R8 AGGRESSIVE OPTIMIZATION ====================
-keep class * extends android.app.Activity
-keep class * extends android.app.Application
-keep class * extends android.app.Service
-keep class * extends android.content.BroadcastReceiver
-keep class * extends android.content.ContentProvider