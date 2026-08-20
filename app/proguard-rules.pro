# Hilt
-keep class dagger.hilt.** { *; }
-keep class com.ashwathai.bubbles.** { *; }
-keep interface dagger.hilt.** { *; }
-dontwarn dagger.hilt.**

# Kotlinx Serialization
-keep class kotlinx.serialization.** { *; }
-keep class com.ashwathai.bubbles.**$$serializer { *; }

# Google Mobile Ads
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.android.gms.ads.identifier.** { *; }

# Datastore
-keep class androidx.datastore.** { *; }

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Coroutines
-keep class kotlinx.coroutines.** { *; }