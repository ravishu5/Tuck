# Proguard rules for Tuck
-keepattributes *Annotation*
-dontwarn javax.annotation.**

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Jsoup
-keep public class org.jsoup.** { public *; }

# ML Kit
-keep class com.google.mlkit.vision.text.** { *; }
