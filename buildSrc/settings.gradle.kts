fun findRnToml(start: File, maxDepth: Int): File? {
    var cursor: File? = start
    repeat(maxDepth) {
        val candidate = File(
            cursor,
            "node_modules/@react-native/gradle-plugin/gradle/libs.versions.toml"
        )
        println("Checking: ${candidate.absolutePath}")
        if (candidate.exists()) return candidate
        cursor = cursor?.parentFile
    }
    return null
}

var rnCatalogPath = findRnToml(settingsDir, 10)?.absolutePath

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
        if (rnCatalogPath != null) {
            create("ssalibs") {
                from(files(rnCatalogPath!!)) }
        }
    }
}
