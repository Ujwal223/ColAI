import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import org.gradle.api.artifacts.CacheableRule
import org.gradle.api.artifacts.ComponentMetadataContext
import org.gradle.api.artifacts.ComponentMetadataRule
import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

@CacheableRule
abstract class RemoveGmsDependenciesRule : ComponentMetadataRule {
    override fun execute(context: ComponentMetadataContext) {
        context.details.allVariants {
            withDependencies {
                removeAll { it.group == "com.google.android.gms" }
            }
        }
    }
}

abstract class StripGeckoWebAuthn : TransformAction<TransformParameters.None> {
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    @get:InputArtifact
    abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val input = inputArtifact.get().asFile
        if (!input.name.startsWith("geckoview-omni")) {
            outputs.file(input)
            return
        }
        val drop = setOf(
            "org/mozilla/geckoview/WebAuthnTokenManager.class",
            "org/mozilla/gecko/util/WebAuthnUtils.class"
        )
        val target = outputs.file(input.name)
        ZipFile(input).use { aar ->
            ZipOutputStream(target.outputStream().buffered()).use { zo ->
                for (e in aar.entries()) {
                    zo.putNextEntry(ZipEntry(e.name))
                    if (e.name == "classes.jar") {
                        val jar = ZipOutputStream(zo)
                        ZipInputStream(aar.getInputStream(e)).use { zi ->
                            generateSequence { zi.nextEntry }.forEach { je ->
                                if (je.name !in drop) {
                                    jar.putNextEntry(ZipEntry(je.name))
                                    zi.copyTo(jar)
                                    jar.closeEntry()
                                }
                            }
                        }
                        jar.finish()
                    } else if (!e.isDirectory) {
                        aar.getInputStream(e).use { it.copyTo(zo) }
                    }
                    zo.closeEntry()
                }
            }
        }
    }
}

android {
    namespace = "com.ujwal.colai"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ujwal.colai"
        minSdk = 26
        targetSdk = 36
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

val geckoStripped = Attribute.of("colai.geckoStripped", Boolean::class.javaObjectType)

configurations.configureEach {
    exclude(group = "com.google.android.gms")
    exclude(group = "com.google.android.gms", module = "play-services-fido")
    exclude(group = "com.google.android.gms", module = "play-services-tasks")
    exclude(group = "com.google.android.gms", module = "play-services-basement")
    if (isCanBeResolved) attributes.attribute(geckoStripped, true)
}

dependencies {
    attributesSchema { attribute(geckoStripped) }
    artifactTypes.maybeCreate("aar").attributes.attribute(geckoStripped, false)
    registerTransform(StripGeckoWebAuthn::class.java) {
        from.attribute(geckoStripped, false)
            .attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "aar")
        to.attribute(geckoStripped, true)
            .attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "aar")
    }

    components {
        all<RemoveGmsDependenciesRule>()
    }

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
