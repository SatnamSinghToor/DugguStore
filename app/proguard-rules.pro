# Keep Ktor debug detector (references JVM-only java.lang.management which isn't on Android)
-keep class io.ktor.util.debug.IntellijIdeaDebugDetector { *; }
-keepclassmembers class io.ktor.** {
    *;
}
-keep class io.ktor.** { *; }

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

# Supabase
-keep class io.github.jan.supabase.** { *; }
