plugins { id("com.android.library") }
android {
 namespace = "com.shilapi.carbridge.ecarx"
 compileSdk { version = release(37) }
 defaultConfig { minSdk = 28 }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_11; targetCompatibility = JavaVersion.VERSION_11 }
}
