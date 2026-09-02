# Supabase & Ktor
-keep class io.github.jan.supabase.** { *; }
-keep class io.ktor.** { *; }
-keep class org.slf4j.** { *; }
-dontwarn org.slf4j.**
-dontwarn org.slf4j.impl.**

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses, Signature
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers @kotlinx.serialization.Serializable class com.duggustore.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.duggustore.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Models
-keepclassmembers class com.duggustore.app.data.model.** { *; }
