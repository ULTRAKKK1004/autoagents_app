# OkHttp
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keep,includedescriptorclasses class com.autoagents.app.**$$serializer { *; }
-keepclassmembers class com.autoagents.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.autoagents.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room
-keep class androidx.room.** { *; }

# Jsoup
-keeppackagenames org.jsoup.nodes
