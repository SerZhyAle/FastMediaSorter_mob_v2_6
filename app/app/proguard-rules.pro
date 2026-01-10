# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the Android SDK tools proguard-android-optimize.txt file.

# ===== Hilt / Dagger =====
-keepclasseswithmembers class * {
    @dagger.hilt.* <methods>;
}
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel { *; }

# ===== Room Database =====
-keep class com.sza.fastmediasorter.data.db.entity.** { *; }
-keep class com.sza.fastmediasorter.data.db.dao.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }

# ===== Domain Models (Parcelable) =====
-keep class com.sza.fastmediasorter.domain.model.** { *; }
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# ===== Glide =====
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
 <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
-keep class com.bumptech.glide.integration.okhttp3.OkHttpGlideModule { *; }

# ===== ExoPlayer / Media3 =====
-keep class androidx.media3.** { *; }
-keep interface androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ===== Network Libraries =====
# SMBJ
-keep class com.hierynomus.smbj.** { *; }
-keep class com.hierynomus.mssmb2.** { *; }
-keep class com.hierynomus.protocol.** { *; }
-dontwarn com.hierynomus.**

# SSHJ
-keep class net.schmizz.sshj.** { *; }
-keep class net.schmizz.concurrent.** { *; }
-dontwarn net.schmizz.**

# Apache Commons Net (FTP)
-keep class org.apache.commons.net.** { *; }
-dontwarn org.apache.commons.net.**

# ===== Security / Crypto =====
# Bouncy Castle (for SMBJ/SSHJ encryption)
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# AndroidX Security Crypto
-keep class androidx.security.crypto.** { *; }

# ===== Kotlin =====
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# ===== Coroutines =====
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ===== DataStore =====
-keep class androidx.datastore.*.** { *; }

# ===== Enum Classes =====
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ===== Remove Logging in Release =====
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
-assumenosideeffects class timber.log.Timber {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# ===== General Android =====
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes Exceptions

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# ViewBinding
-keep class * implements androidx.viewbinding.ViewBinding {
    public static *** bind(***);
    public static *** inflate(***);
}

# ===== Suppress Warnings for Optional Dependencies =====
# MBassador (SMBJ internal - optional EL support)
-dontwarn javax.el.**

# EdDSA / Bouncy Castle internals
-dontwarn sun.security.x509.**

# SSHJ internal optional dependencies
-dontwarn net.i2p.crypto.eddsa.**
-dontwarn org.slf4j.**
-dontwarn ch.qos.logback.**

# ===== FindBugs Annotations (Microsoft Identity Library) =====
# These annotations are compile-time only and not needed at runtime
-dontwarn edu.umd.cs.findbugs.annotations.**

# ===== PDF Box Optional Dependencies =====
# JP2 codec is optional for JPEG2000 support in PDFs
-dontwarn com.gemalto.jp2.JP2Decoder
-dontwarn com.gemalto.jp2.JP2Encoder
