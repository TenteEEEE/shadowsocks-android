// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    alias(libs.plugins.versions)
    alias(libs.plugins.ksp) apply false
}

buildscript {
    apply(from = "repositories.gradle.kts")

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }

    val rn = the<VersionCatalogsExtension>().find("ssalibs")

    dependencies {
        rn.ifPresentOrElse({
            println("YESSSSSSS")
            classpath(it.findLibrary("android-gradle-plugin").get())
        },{
            classpath(libs.android.gradle)
        })
        //classpath(libs.android.gradle)
        classpath(libs.dokka)
        classpath(libs.google.oss.licenses)
        classpath(libs.gradle.maven.publish)
        classpath(libs.kotlin.gradle)
        classpath(libs.rust.android)
    }
}

allprojects {
    apply(from = "${rootProject.projectDir}/repositories.gradle.kts")
    group = "com.github.shadowsocks"
    version = "5.3.4"
}


