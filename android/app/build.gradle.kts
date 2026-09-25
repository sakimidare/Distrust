plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
        )
    }
}

android {
    namespace = "idont.trust.atrust"
    compileSdk = 37

    defaultConfig {
        applicationId = "idont.trust.atrust"
        minSdk = 26
        targetSdk = 37
        versionCode = 3
        versionName = "0.1.3a"

        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        debug {
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("debug")
            ndk {
                abiFilters += "arm64-v8a"
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = false
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
        jniLibs.useLegacyPackaging = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.08.00")

    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.datastore:datastore-preferences:1.2.1")
    implementation("androidx.compose.material3:material3:1.5.0-alpha28")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("top.yukonga.miuix.kmp:miuix-nav:0.9.4-rc01")
    implementation("com.materialkolor:material-kolor:5.0.1")
    implementation("com.github.KieronQuinn:MonetCompat:0.4.1")
    implementation("com.google.android.material:material:1.14.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")

    debugImplementation(composeBom)
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation(files(rootProject.file("core/build/distrust-core.aar")))
}

val buildGoCore by tasks.registering(Exec::class) {
    group = "build"
    description = "Build the pinned DistrustCore source into an Android AAR"
    workingDir(rootProject.projectDir)
    commandLine("bash", "scripts/build-go-core.sh")
    val releaseBuild = gradle.startParameter.taskNames.any { it.contains("release", ignoreCase = true) }
    val coreBuildMode = if (releaseBuild) "release-arm64-min" else "debug-all"
    inputs.property("coreBuildMode", coreBuildMode)
    if (releaseBuild) {
        environment("DISTRUST_CORE_TARGET", "android/arm64")
        environment("DISTRUST_CORE_TRIMPATH", "true")
        environment("DISTRUST_CORE_LDFLAGS", "-s -w -buildid=")
        // The same output path is also used by debug-all builds. A release invocation must
        // always replace it so a stale four-ABI, symbol-rich AAR cannot leak into the APK.
        outputs.upToDateWhen { false }
    }
    inputs.files(
        rootProject.fileTree("core") {
            exclude(".git/**", "build/**")
        },
        rootProject.file("scripts/build-go-core.sh"),
    )
    outputs.file(rootProject.file("core/build/distrust-core.aar"))
}

tasks.named("preBuild") {
    if (!providers.gradleProperty("skipGoCore").isPresent) {
        dependsOn(buildGoCore)
    }
}
