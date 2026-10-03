plugins { id("com.android.library") version "8.7.3" }
repositories { mavenCentral(); google() }
android {
    namespace = "cn.manstep.phonemirrorBox.bridge.tests"
    compileSdk = 34
    defaultConfig { minSdk = 28 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    sourceSets.getByName("main").java.setSrcDirs(listOf("../src", "../stubs"))
    sourceSets.getByName("main").manifest.srcFile("AndroidManifest.xml")
    sourceSets.getByName("test").java.setSrcDirs(listOf("../tests"))
    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            it.maxHeapSize = "2g"
            it.systemProperty("robolectric.dependency.repo.url", "https://repo.maven.apache.org/maven2")
            it.systemProperty("java.io.tmpdir", System.getProperty("java.io.tmpdir"))
        }
    }
}
dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
}
