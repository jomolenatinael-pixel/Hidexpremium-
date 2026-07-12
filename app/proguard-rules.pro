# ==========================================================================
# HideX Vault Pro — ProGuard / R8 rules for production release builds
# ==========================================================================

# --- Kotlin metadata & reflection ---
-keepattributes *Annotation*, SourceFile, LineNumberTable, Signature, Exceptions, InnerClasses, EnclosingMethod, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$$serializer { *; }
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Room ---
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep class androidx.room.** { *; }
-dontwarn androidx.room.paging.**

# --- Moshi ---
-keepclassmembers class ** {
    @com.squareup.moshi.* <methods>;
}
-keep @com.squareup.moshi.JsonQualifier @interface *
-keep class **JsonAdapter { <init>(...); *; }
-keep class kotlin.Metadata { *; }
-keepclassmembers,allowobfuscation class * {
    @com.squareup.moshi.* <fields>;
}

# --- Retrofit / OkHttp ---
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }
-dontwarn okio.**
-keep class okio.** { *; }
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# --- Coroutines ---
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** { volatile <fields>; }
-dontwarn kotlinx.coroutines.debug.**

# --- Jetpack Compose ---
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
-keep class androidx.compose.runtime.** { *; }

# --- DataStore ---
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# --- Navigation ---
-keep class androidx.navigation.** { *; }
-dontwarn androidx.navigation.**

# --- Coil (image loading) ---
-keep class coil.** { *; }
-dontwarn coil.**

# --- Firebase ---
-keep class com.google.firebase.** { *; }
-keep class com.google.ai.client.generativeai.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.ai.**

# --- AndroidX core / lifecycle ---
-keep class androidx.lifecycle.** { *; }
-dontwarn androidx.lifecycle.**
-keep class androidx.core.** { *; }

# --- App model classes (entities, keep field names for Room/Moshi) ---
-keep class com.example.data.** { *; }
-keep class com.example.data.api.** { *; }

# --- SecurityManager (uses reflection-free crypto, keep it safe) ---
-keep class com.example.security.SecurityManager { *; }

# --- JSON parsing (org.json) ---
-keep class org.json.** { *; }

# --- Keep enum values ---
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
