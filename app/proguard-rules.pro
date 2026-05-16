# ProGuard / R8 rules for release builds.
# Defaults in proguard-android-optimize.txt handle most of what we need;
# this file only lists things R8 can't figure out on its own.

# --- Apollo GraphQL -----------------------------------------------------------
# Generated query/mutation classes + their inner types are reflected over by
# Apollo's runtime via the `CompiledField` machinery.
-keep class io.stashapp.android.graphql.** { *; }
-keep class io.stashapp.android.graphql.type.** { *; }
-keep class io.stashapp.android.graphql.fragment.** { *; }

# --- Hilt / Dagger ------------------------------------------------------------
-keepnames class dagger.hilt.internal.aggregatedroot.codegen.* { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# --- Media3 -------------------------------------------------------------------
# Media3 loads extension renderers via reflection (FfmpegLibrary etc.).
-keep class androidx.media3.** { *; }
-keep class io.github.anilbeesetti.nextlib.** { *; }
-dontwarn androidx.media3.**
-dontwarn io.github.anilbeesetti.nextlib.**

# --- kotlinx.serialization ----------------------------------------------------
# Our UiPreferences uses @Serializable on a private class; R8 with default
# rules handles this but be explicit to avoid future breakage.
-keepclassmembers @kotlinx.serialization.Serializable class * {
    static **$* Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class **$$serializer { *; }

# --- Compose preview / Tooling ------------------------------------------------
# Not strictly needed for release but keeps ComposePreview functional when
# the release APK is inspected via `adb shell am start-activity -c debug`.
-keep class androidx.compose.runtime.** { *; }

# --- Kotlinx coroutines -------------------------------------------------------
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# --- OkHttp -------------------------------------------------------------------
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# --- Stash domain / data models used via reflection by DataStore serializer --
-keep class io.stashapp.android.core.model.** { *; }
-keep class io.stashapp.android.core.domain.** { *; }
