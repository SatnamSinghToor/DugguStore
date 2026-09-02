# Supabase & Ktor
-keep class io.github.jan.supabase.** { *; }
-keep class io.ktor.** { *; }
-keep class org.slf4j.** { *; }
-dontwarn org.slf4j.**
-dontwarn org.slf4j.impl.**
-dontwarn java.lang.management.**
-dontwarn io.ktor.util.debug.**
-dontwarn io.ktor.client.features.**
-dontwarn io.ktor.client.engine.**
-dontwarn io.ktor.serialization.**

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
