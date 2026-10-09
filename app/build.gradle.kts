plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.ujwal.colai"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ujwal.colai"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "2.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86_64"))
        }
    }

    androidResources {
        // Keep English resources only, stripping all third-party localization bloat
        localeFilters += listOf("en")
    }

    signingConfigs {
        val keystoreFile = file("release-keystore.jks")
        if (keystoreFile.exists()) {
            val sPassword = System.getenv("KEYSTORE_PASSWORD")?.trim()?.ifEmpty { null }
                ?: System.getenv("ANDROID_KEYSTORE_PASSWORD")?.trim()?.ifEmpty { null }
                ?: (findProperty("KEYSTORE_PASSWORD") as? String)?.trim()?.ifEmpty { null }
                ?: "android"

            val kAlias = System.getenv("KEY_ALIAS")?.trim()?.ifEmpty { null }
                ?: System.getenv("ANDROID_KEY_ALIAS")?.trim()?.ifEmpty { null }
                ?: (findProperty("KEY_ALIAS") as? String)?.trim()?.ifEmpty { null }
                ?: "colai"

            val kPassword = System.getenv("KEY_PASSWORD")?.trim()?.ifEmpty { null }
                ?: System.getenv("ANDROID_KEY_PASSWORD")?.trim()?.ifEmpty { null }
                ?: (findProperty("KEY_PASSWORD") as? String)?.trim()?.ifEmpty { null }
                ?: sPassword

            create("release") {
                storeFile = keystoreFile
                storePassword = sPassword
                keyAlias = kAlias
                keyPassword = kPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
        debug {
            isMinifyEnabled = false
            isDebuggable = true
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }


    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/*.kotlin_module"
            excludes += "META-INF/*.version"
            excludes += "META-INF/androidx/**"
            excludes += "**/*.properties"
            excludes += "**/*.proto"
            excludes += "META-INF/INDEX.LIST"
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }

    sourceSets {
        getByName("main") {
            assets.srcDirs("src/main/assets")
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86_64")
            isUniversalApk = true
        }
    }

    // Assign distinct versionCode multiplier per ABI so 64-bit devices prioritize 64-bit builds
    // Universal: 1, armeabi-v7a: 2, arm64-v8a: 3, x86_64: 4
    val abiVersionCodes = mapOf("armeabi-v7a" to 2, "arm64-v8a" to 3, "x86_64" to 4)

    applicationVariants.all {
        val baseCode = defaultConfig.versionCode ?: 1
        outputs.forEach { output ->
            if (output is com.android.build.gradle.internal.api.ApkVariantOutputImpl) {
                val abi = output.getFilter("ABI")
                val abiOffset = abiVersionCodes[abi] ?: 1
                output.versionCodeOverride = baseCode * 10 + abiOffset
            }
        }
    }
}

configurations.all {
    exclude(group = "com.google.android.gms")
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // AndroidX & Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Jetpack Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    debugImplementation(libs.androidx.compose.ui.tooling)

    // Mozilla GeckoView Engine
    implementation(libs.mozilla.geckoview) {
        exclude(group = "com.google.android.gms")
    }

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Jetpack Security
    implementation(libs.androidx.security.crypto)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Unit Testing
    testImplementation(libs.junit)
    testImplementation(libs.json)
    testImplementation(libs.kotlinx.coroutines.test)

    // Instrumented Testing
    androidTestImplementation(libs.junit)
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

tasks.matching { it.name.contains("AarMetadata") }.configureEach {
    enabled = false
}

configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlin:kotlin-stdlib:2.0.21")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.0.21")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:2.0.21")
        force("org.jetbrains.kotlin:kotlin-stdlib-common:2.0.21")
    }
}
