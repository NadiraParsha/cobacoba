// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // Plugin untuk aplikasi Android, dengan versi yang stabil.
    id("com.android.application") version "8.2.2" apply false

    // Plugin untuk bahasa Kotlin di Android, dengan versi yang sesuai.
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false

    // Plugin untuk Firebase, dengan versi yang stabil dan telah teruji.
    id("com.google.gms.google-services") version "4.4.1" apply false
}
