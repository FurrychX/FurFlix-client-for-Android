# Proguard rules for FurFlix

-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses,EnclosingMethod

# Keep JSoup
-keeppackagenames org.jsoup.nodes

# Keep data models
-keep class com.furflix.app.data.model.** { *; }

# Keep Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers @kotlinx.serialization.Serializable class com.furflix.app.data.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.furflix.app.data.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
