plugins {
    `kotlin-dsl`
}

apply(from = "../repositories.gradle.kts")

val rn = the<VersionCatalogsExtension>().find("ssalibs")

dependencies {
    rn.ifPresentOrElse({
        implementation(it.findLibrary("android-gradle-plugin").get())
    },{
        implementation(libs.android.gradle)
    })
    implementation(libs.kotlin.gradle)
}

tasks.register("checkRnFromBuildSrc") {
    doLast {
        rn.ifPresent {
            println("AGP    -> " + it.findVersion("agp").get().requiredVersion)
            println("Kotlin -> " + it.findVersion("kotlin").get().requiredVersion)
        }
    }
}
