
import java.util.Properties
import java.io.FileInputStream
import java.io.File


plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
    alias(libs.plugins.kotlinKapt)
}

// Function to safely read API keys from local.properties
fun getApiKey(projectRootDir: File, propertyKey: String): String {
    val properties = Properties()
    val localPropertiesFile = File(projectRootDir, "local.properties")
    if (localPropertiesFile.exists() && localPropertiesFile.isFile) {
        try {
            properties.load(FileInputStream(localPropertiesFile))
            return properties.getProperty(propertyKey, "")
        } catch (e: Exception) {
            println("Warning: Could not load local.properties file: ${e.message}")
        }
    } else {
        println("Warning: local.properties file not found at ${localPropertiesFile.absolutePath}. API keys will be empty.")
    }
    return ""
}

android {
    namespace = "com.example.traveltracker"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.traveltracker"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Access API keys from local.properties
        val mapsApiKey = getApiKey(rootProject.rootDir, "MAPS_API_KEY")
        val geminiApiKey = getApiKey(rootProject.rootDir, "GEMINI_API_KEY")
        if (mapsApiKey.isEmpty()) { println("Warning: MAPS_API_KEY is empty...") }
        if (geminiApiKey.isEmpty()) { println("Warning: GEMINI_API_KEY is empty...") }
        buildConfigField("String", "MAPS_API_KEY", "\"$mapsApiKey\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

            val mapsApiKey = getApiKey(rootProject.rootDir, "MAPS_API_KEY")
            val geminiApiKey = getApiKey(rootProject.rootDir, "GEMINI_API_KEY")
            buildConfigField("String", "MAPS_API_KEY", "\"$mapsApiKey\"")
            buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
            manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
        }
        debug {

            isDebuggable = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    kapt {
        correctErrorTypes = true
    }
}

dependencies {

    // --- Core AndroidX ---
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.livedata.ktx)

    // --- Firebase ---
    implementation(platform("com.google.firebase:firebase-bom:33.2.0"))

    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation(libs.firebase.storage)
    implementation("com.google.firebase:firebase-analytics")

    // *** Added Firebase App Check ***
    implementation("com.google.firebase:firebase-appcheck-playintegrity")

    // --- Google Maps ---
    implementation(libs.play.services.maps)

    // --- Gemini AI ---
    implementation(libs.google.ai.generativelanguage)

    // --- Glide (Image Loading) ---
    implementation(libs.glide)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    annotationProcessor(libs.glide.compiler)

    // --- Testing ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}