plugins {
    id("com.android.application")
}

val releaseStoreFile = providers.gradleProperty("MEDIABRIDGE_STORE_FILE")
    .orElse(providers.environmentVariable("MEDIABRIDGE_STORE_FILE"))
val releaseStorePassword = providers.gradleProperty("MEDIABRIDGE_STORE_PASSWORD")
    .orElse(providers.environmentVariable("MEDIABRIDGE_STORE_PASSWORD"))
val releaseKeyAlias = providers.gradleProperty("MEDIABRIDGE_KEY_ALIAS")
    .orElse(providers.environmentVariable("MEDIABRIDGE_KEY_ALIAS"))
val releaseKeyPassword = providers.gradleProperty("MEDIABRIDGE_KEY_PASSWORD")
    .orElse(providers.environmentVariable("MEDIABRIDGE_KEY_PASSWORD"))
val hasReleaseSigning = listOf(
    releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword
).all { it.isPresent }

android {
    namespace = "com.mediabridge.app"
    compileSdk = 34
    buildFeatures { buildConfig = true }

    defaultConfig {
        applicationId = "com.mediabridge.app"
        minSdk = 28
        targetSdk = 34
        versionCode = 26100302
        versionName = "2.3.9-usbbox-26100302"
        testInstrumentationRunner = "android.test.InstrumentationTestRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("localRelease") {
                storeFile = file(releaseStoreFile.get())
                storePassword = releaseStorePassword.get()
                keyAlias = releaseKeyAlias.get()
                keyPassword = releaseKeyPassword.get()
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
        }
        release {
            isMinifyEnabled = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("localRelease")
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            it.systemProperty("robolectric.dependency.repo.url", "https://repo.maven.apache.org/maven2")
            it.maxHeapSize = "2g"
        }
    }
    sourceSets.getByName("test").resources.srcDir("../protocol-fixtures")

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
}
