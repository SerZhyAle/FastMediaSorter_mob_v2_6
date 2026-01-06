# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the Android SDK tools proguard-android-optimize.txt file.

# Keep Hilt
-keepclasseswithmembers class * {
    @dagger.hilt.* <methods>;
}

# Keep Room entities
-keep class com.sza.fastmediasorter.data.db.entity.** { *; }

# Keep Glide generated API
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
 <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

# ExoPlayer
-keep class androidx.media3.** { *; }

# SMB/SFTP/FTP Clients (for Epic 4)
-keep class com.hierynomus.smbj.** { *; }
-keep class net.schmizz.sshj.** { *; }
-keep class org.apache.commons.net.** { *; }

# Bouncy Castle (for SMBJ/SSHJ)
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**
