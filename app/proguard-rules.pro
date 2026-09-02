# Supabase
-keep class io.github.jan.supabase.** { *; }
-keepclassmembers class com.duggustore.app.data.model.** { *; }
-keepattributes *Annotation*
-keepattributes Signature

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers @kotlinx.serialization.Serializable class com.duggustore.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.duggustore.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
