plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.20" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.20" apply false
    // Reads app/google-services.json and wires it into the build — needed
    // for Firebase Cloud Messaging (push notifications).
    id("com.google.gms.google-services") version "4.4.2" apply false
}
