# Hilt
-keep class dagger.hilt.** { *; }
-keep class com.ashwathai.bubbles.** { *; }
-keep interface dagger.hilt.** { *; }
-dontwarn dagger.hilt.**

# Kotlinx Serialization
-keep class kotlinx.serialization.** { *; }
-keep class com.ashwathai.bubbles.**$$serializer { *; }

# LevelPlay / IronSource
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
-keep class com.ironsource.** { *; }
-keep class com.unity3d.ironsource.** { *; }
-dontwarn com.ironsource.**
-dontwarn com.unity3d.ironsource.**
-keep class androidx.recyclerview.widget.RecyclerView { *; }
-keep class androidx.recyclerview.widget.RecyclerView$OnScrollListener { *; }
# Keep mediated network adapters if added later
-keep class * extends android.app.Activity

# Datastore
-keep class androidx.datastore.** { *; }

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Coroutines
-keep class kotlinx.coroutines.** { *; }