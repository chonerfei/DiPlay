plugins {
    id("com.android.library")
}

android {
    namespace = "com.shilapi.xcertplay.host"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 19
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // Pre-existing findings unrelated to the Android 4.4 API-compat port: untranslated
    // resources, androidx RestrictedApi use, permission annotations, format strings.
    // NewApi stays an error so future API-compat regressions still fail the build.
    lint {
        warning += listOf(
            "MissingTranslation",
            "MissingPermission",
            "RestrictedApi",
            "StringFormatMatches",
            "StringFormatInvalid",
            "ForegroundServicePermission",
            "UnspecifiedRegisterReceiverFlag",
        )
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        // The UI suite covers several SDKs and locale-specific resource sandboxes.
        unitTests.all { it.maxHeapSize = "1g" }
    }
}

dependencies {
    api(project(":shared"))
    // Compose, media3 and activity-compose removed for the Android 4.4 wired-only port.
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    testImplementation("org.robolectric:robolectric:4.17")
    testImplementation("org.mockito:mockito-core:5.20.0")
    testImplementation(libs.jmdns)
}
