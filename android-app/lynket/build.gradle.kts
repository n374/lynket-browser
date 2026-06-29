/*
 *  Lynket
 *
 *  Copyright (C) 2025 Arunkumar
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinParcelize)
    alias(libs.plugins.kotlinKapt) // Will be removed after KSP migration
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt) // Phase 1.1: Enabled for modernization
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "arun.com.chromer"
    compileSdk = 35

    defaultConfig {
        applicationId = "arun.com.chromer"
        minSdk = 24 // Android 7.0+ (Decision: Bump from 23)
        targetSdk = 35 // Phase 1.1: Updated to latest
        versionCode = 56
        versionName = "2.1.3"

        testInstrumentationRunner = "arun.com.chromer.HiltTestRunner" // Phase 1.1: Hilt test runner

        vectorDrawables {
            useSupportLibrary = true
        }

        multiDexEnabled = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.FlowPreview",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi"
        )
    }

    buildFeatures {
        buildConfig = true
        compose = true
        viewBinding = true // Temporary - will be removed after migration
    }

    buildTypes {
        release {
            resValue("string", "app_name", "Lynket")
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = " - dev"
            resValue("string", "app_name", "Lynket-dev")
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = true
        }
    }

    packaging {
        resources {
            excludes += listOf(
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
                "META-INF/licenses/**",
                "META-INF/rxjava.properties"
            )
        }
    }

    lint {
        abortOnError = false
        disable += "MissingTranslation"
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.multidex)
    implementation(libs.material)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.extensions)

    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material3.window)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Hilt Dependency Injection - Phase 1.1: Enabled
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Hilt Testing
    androidTestImplementation(libs.hilt.android.testing)
    kaptAndroidTest(libs.hilt.android.testing)
    kaptAndroidTest(libs.hilt.compiler)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    ksp(libs.androidx.room.compiler)

    // DataStore
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.datastore.core)

    // Paging 3
    implementation(libs.androidx.paging.runtime.ktx)
    implementation(libs.androidx.paging.compose)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)
    ksp(libs.moshi.codegen)

    // Image Loading - Coil (Modern)
    implementation(libs.coil)
    implementation(libs.coil.compose)

    // Logging
    implementation(libs.timber)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.mockk)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)

    // =============================================
    // LEGACY DEPENDENCIES - TO BE REMOVED
    // Keep temporarily during migration (Steps 3-9)
    // =============================================

    // Legacy: Dagger 2 (will coexist with Hilt temporarily)
    implementation("com.google.dagger:dagger:2.50")
    kapt("com.google.dagger:dagger-compiler:2.50")
    kaptTest("com.google.dagger:dagger-compiler:2.50")

    // Legacy: Image Loading - Glide
    implementation(libs.glide)
    kapt(libs.glide.compiler)

    // Legacy: View Binding - Butterknife
    implementation(libs.butterknife)
    kapt(libs.butterknife.compiler)

    // Legacy: RecyclerView - Epoxy
    implementation(libs.epoxy)
    kapt(libs.epoxy.processor)

    // Legacy: RxJava 1.x & 2.x
    implementation("io.reactivex:rxjava:1.3.8")
    implementation("io.reactivex:rxandroid:1.2.1")
    implementation("com.jakewharton.rxbinding:rxbinding:1.0.1")
    implementation("com.github.akarnokd:rxjava2-interop:0.13.7")
    implementation(libs.rxjava)
    implementation(libs.rxandroid)
    implementation(libs.rxkotlin)
    implementation(libs.rxrelay)
    implementation("com.jakewharton.rxbinding3:rxbinding:3.1.0")
    implementation("com.jakewharton.rxbinding3:rxbinding-appcompat:3.1.0")
    implementation("com.jakewharton.rxbinding3:rxbinding-recyclerview:3.1.0")
    implementation("com.uber.rxdogtag:rxdogtag:0.3.0")

    // Legacy: RxPrefs
    implementation("com.afollestad:rxkprefs:1.2.5")

    // Legacy: Storage - PaperDB
    implementation("io.github.pilgr:paperdb:2.7.2")

    // Legacy: UI Libraries
    implementation("com.afollestad.material-dialogs:core:0.9.6.0")
    implementation("com.afollestad.material-dialogs:commons:0.9.6.0")
    implementation("com.github.apl-devs:appintro:v4.2.3")
    implementation("com.mikepenz:materialdrawer:6.1.2@aar") {
        isTransitive = true
    }
    implementation("com.mikepenz:community-material-typeface:2.0.46.1@aar")
    implementation("com.github.chrisbanes:PhotoView:2.3.0")

    // Legacy: AndroidX (old versions, will be updated)
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.legacy:legacy-preference-v14:1.0.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.palette:palette-ktx:1.0.0")
    implementation("androidx.dynamicanimation:dynamicanimation:1.0.0")
    implementation("androidx.sqlite:sqlite-ktx:2.0.1")
    implementation("androidx.fragment:fragment-ktx:1.0.0")

    // Legacy: Paging 2.x (will be migrated to Paging 3)
    implementation("androidx.paging:paging-runtime-ktx:2.1.2")

    // Legacy: Other
    implementation("com.facebook.rebound:rebound:0.3.8")
    implementation("com.github.nekocode:Badge:1.2")
    implementation("com.github.duanhong169:drawabletoolbox:1.0.7")

    // Disk LRU cache module
    implementation(project(":disk-cache"))
}

// KAPT Configuration
kapt {
    correctErrorTypes = true
    javacOptions {
        option("-Xmaxerrs", 500)
    }
}

// Exclude payments package from compilation (uses deprecated In-App Billing API)
tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Xlint:-path")
    exclude("**/payments/**")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    exclude("**/payments/**")
}
