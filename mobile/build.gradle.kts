import java.util.Properties

plugins {
    id("com.android.application")
    kotlin("android")
    id("kotlin-parcelize")
}

setupApp()

android {
    namespace = "com.github.shadowsocks"
    defaultConfig.applicationId = "com.github.shadowsocks"

    val keystorePropertiesPath = providers.gradleProperty("ssKeystoreProperties")
        .orElse(providers.environmentVariable("SS_KEYSTORE_PROPERTIES"))
        .orElse("${System.getProperty("user.home")}/.android-signing/shadowsocks-keystore.properties")
        .get()
    val keystorePropertiesFile = file(keystorePropertiesPath)
    if (keystorePropertiesFile.isFile) {
        val keystoreProperties = Properties().apply {
            keystorePropertiesFile.inputStream().use { load(it) }
        }
        signingConfigs.create("release") {
            val storePath: String = keystoreProperties.getProperty("storeFile")
            storeFile = (if (File(storePath).isAbsolute) File(storePath)
                else File(keystorePropertiesFile.parentFile, storePath))
            storePassword = keystoreProperties.getProperty("storePassword")
            keyAlias = keystoreProperties.getProperty("keyAlias")
            keyPassword = keystoreProperties.getProperty("keyPassword")
        }
        buildTypes.getByName("release").signingConfig = signingConfigs.getByName("release")
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.fastscroll)
    implementation(libs.locale.api)
    implementation(libs.preferencex.simplemenu)
    implementation(libs.zxing)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.test.runner)
}
