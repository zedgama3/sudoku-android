plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Short commit the build came from, shown on the About screen. "-dirty" marks uncommitted
// changes; "unknown" means the source wasn't a git checkout (or git isn't installed).
fun git(vararg args: String): String? = runCatching {
    providers.exec { commandLine("git", *args) }.standardOutput.asText.get().trim()
}.getOrNull()
val gitSha = git("rev-parse", "--short", "HEAD")
    ?.let { sha -> if (git("status", "--porcelain").isNullOrEmpty()) sha else "$sha-dirty" }
    ?: "unknown"

android {
    namespace = "net.zg3.sudoku"
    compileSdk = 37

    defaultConfig {
        applicationId = "net.zg3.sudoku"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "GIT_SHA", "\"$gitSha\"")
    }

    // Upload key for Play, read from ~/.gradle/gradle.properties (see DEPLOYING.md). Without it
    // the release build is left unsigned, so the project still builds for everyone else.
    val uploadStoreFile = providers.gradleProperty("SUDOKU_UPLOAD_STORE_FILE").orNull
    signingConfigs {
        if (uploadStoreFile != null) {
            create("release") {
                storeFile = file(uploadStoreFile)
                storePassword = providers.gradleProperty("SUDOKU_UPLOAD_STORE_PASSWORD").get()
                keyAlias = providers.gradleProperty("SUDOKU_UPLOAD_KEY_ALIAS").get()
                keyPassword = providers.gradleProperty("SUDOKU_UPLOAD_KEY_PASSWORD").get()
            }
        }
    }

    buildTypes {
        release {
            signingConfigs.findByName("release")?.let { signingConfig = it }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.navigation.compose)
    implementation(libs.datastore)
    implementation(libs.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
