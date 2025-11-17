plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    kotlin("plugin.serialization") version "2.1.21"
    id("com.google.protobuf") version ("0.9.5")
}

android {
    namespace = "ttgt.schedule"
    compileSdk = 36

    defaultConfig {
        applicationId = "ttgt.schedule"
        minSdk = 24
        targetSdk = 36
        versionCode = 5
        versionName = "5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            isMinifyEnabled = true
            versionNameSuffix = " (debug)"
            applicationIdSuffix = ".debug"
            proguardFiles(
                "proguard-rules.pro",
                //getDefaultProguardFile("proguard-android-optimize.txt")
            )
        }

        release {
            isMinifyEnabled = true
            proguardFiles(
                //getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.32.0"
    }
    plugins {
        create("java") {
            artifact = "com.google.protobuf:protobuf-java:4.32.0"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java")
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.datastore)
    implementation(libs.protoc)

    implementation(libs.protobuf.java)
    implementation(libs.ktor.client.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.cio)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.glance.appwidget)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}