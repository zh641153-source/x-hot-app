plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "com.xhot.app"
    compileSdk = 34
    defaultConfig { applicationId = "com.xhot.app"; minSdk = 24; targetSdk = 34; versionCode = 7; versionName = "1.6" }
    signingConfigs { create("release") { storeFile = file("../release.keystore"); storePassword = System.getenv("XHOT_STORE_PASSWORD") ?: ""; keyAlias = "xhot"; keyPassword = System.getenv("XHOT_KEY_PASSWORD") ?: "" } }
    buildTypes { release { isMinifyEnabled = false; signingConfig = signingConfigs.getByName("release") } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    testImplementation("junit:junit:4.13.2")
}
