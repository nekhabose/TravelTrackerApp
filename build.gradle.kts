// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // Define plugins required by modules but don't apply them here
    alias(libs.plugins.android.application) apply false
    id("com.google.gms.google-services") version "4.4.2" apply false // Define google-services plugin
    alias(libs.plugins.kotlinAndroid) apply false        // Define kotlin plugin (needed for kapt)
    alias(libs.plugins.kotlinKapt) apply false           // Define kapt plugin
}

//// Top-level build file where you can add configuration options common to all sub-projects/modules.
//plugins {
//    alias(libs.plugins.android.application) apply false
//
//}