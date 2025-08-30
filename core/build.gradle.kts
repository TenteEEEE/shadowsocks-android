import com.android.build.gradle.internal.tasks.factory.dependsOn

fun findPythonCommand(): String {
    // gradle.propertiesで明示的に設定されている場合はそれを使用
    val manualPythonCommand = findProperty("rust.python.command")?.toString()
    if (manualPythonCommand != null) {
        try {
            val pythonFile = file(manualPythonCommand)
            if (pythonFile.exists()) {
                val process = ProcessBuilder(manualPythonCommand, "--version")
                    .redirectErrorStream(true)
                    .start()
                val exitCode = process.waitFor()
                
                if (exitCode == 0) {
                    val output = process.inputStream.bufferedReader().readText().trim()
                    println("Using manually configured Python: $manualPythonCommand -> $output")
                    return manualPythonCommand
                }
            }
        } catch (e: Exception) {
            println("Failed to use manually configured Python $manualPythonCommand: ${e.message}")
        }
    }
    
    // 複数のPython検出方法を試行
    val pythonCommands = listOf("python3", "python")
    val commonPythonPaths = listOf(
        "C:\\tools\\miniconda3\\python.exe",
        "C:\\Python39\\python.exe",
        "C:\\Python38\\python.exe",
        "C:\\Python310\\python.exe",
        "C:\\Python311\\python.exe",
        "C:\\Python312\\python.exe",
        "C:\\Users\\${System.getProperty("user.name")}\\miniconda3\\python.exe",
        "C:\\Users\\${System.getProperty("user.name")}\\anaconda3\\python.exe"
    )
    
    // まずPATH内のコマンドを試行
    for (command in pythonCommands) {
        try {
            val process = ProcessBuilder(command, "--version")
                .redirectErrorStream(true)
                .start()
            val exitCode = process.waitFor()
            
            if (exitCode == 0) {
                val output = process.inputStream.bufferedReader().readText().trim()
                println("Found Python in PATH: $command -> $output")
                return command
            }
        } catch (e: Exception) {
            println("Failed to execute $command from PATH: ${e.message}")
        }
    }
    
    // 次に一般的なインストール場所を試行
    for (pythonPath in commonPythonPaths) {
        try {
            val pythonFile = file(pythonPath)
            if (pythonFile.exists()) {
                val process = ProcessBuilder(pythonPath, "--version")
                    .redirectErrorStream(true)
                    .start()
                val exitCode = process.waitFor()
                
                if (exitCode == 0) {
                    val output = process.inputStream.bufferedReader().readText().trim()
                    println("Found Python at fixed path: $pythonPath -> $output")
                    return pythonPath
                }
            }
        } catch (e: Exception) {
            println("Failed to execute $pythonPath: ${e.message}")
        }
    }
    
    // 環境変数からcondaパスを取得して試行
    try {
        val condaPrefix = System.getenv("CONDA_PREFIX")
        if (condaPrefix != null) {
            val condaPython = "$condaPrefix\\python.exe"
            val pythonFile = file(condaPython)
            if (pythonFile.exists()) {
                val process = ProcessBuilder(condaPython, "--version")
                    .redirectErrorStream(true)
                    .start()
                val exitCode = process.waitFor()
                
                if (exitCode == 0) {
                    val output = process.inputStream.bufferedReader().readText().trim()
                    println("Found Python via CONDA_PREFIX: $condaPython -> $output")
                    return condaPython
                }
            }
        }
    } catch (e: Exception) {
        println("Failed to use CONDA_PREFIX: ${e.message}")
    }
    
    throw GradleException("No Python executable found. Please install Python 3.x and ensure it's in your PATH, or install Miniconda/Anaconda.")
}

plugins {
    id("com.android.library")
    id("com.google.devtools.ksp")
    id("org.mozilla.rust-android-gradle.rust-android")
    kotlin("android")
    id("kotlin-parcelize")
}

setupCore()

android {
    namespace = "com.github.shadowsocks.core"

    defaultConfig {
        consumerProguardFiles("proguard-rules.pro")

        externalNativeBuild.ndkBuild {
            abiFilters("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            arguments("-j${Runtime.getRuntime().availableProcessors()}")
        }

        ksp {
            arg("room.incremental", "true")
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }

    externalNativeBuild.ndkBuild.path("src/main/jni/Android.mk")

    sourceSets.getByName("androidTest") {
        assets.setSrcDirs(assets.srcDirs + files("$projectDir/schemas"))
    }

    buildFeatures.aidl = true
}

cargo {
    module = "src/main/rust/shadowsocks-rust"
    libname = "sslocal"
    targets = listOf("arm", "arm64", "x86", "x86_64")
    profile = findProperty("CARGO_PROFILE")?.toString() ?: currentFlavor
    extraCargoBuildArguments = listOf("--bin", libname!!)
    featureSpec.noDefaultBut(arrayOf(
        "stream-cipher",
        "aead-cipher-extra",
        "logging",
        "local-flow-stat",
        "local-dns",
        "aead-cipher-2022",
    ))
    exec = { spec, toolchain ->
        run {
            project.logger.lifecycle("=== Python Detection Debug ===")
            project.logger.lifecycle("User name: ${System.getProperty("user.name")}")
            project.logger.lifecycle("System PATH: ${System.getenv("PATH")}")
            project.logger.lifecycle("CONDA_PREFIX: ${System.getenv("CONDA_PREFIX")}")
            
            val pythonCommand = findPythonCommand()
            spec.environment("RUST_ANDROID_GRADLE_PYTHON_COMMAND", pythonCommand)
            project.logger.lifecycle("Using Python command: $pythonCommand")
            project.logger.lifecycle("=== End Python Detection Debug ===")
            
            // 環境変数の継承を確実にする
            System.getenv().forEach { (key, value) ->
                if (key.startsWith("CONDA") || key == "PATH") {
                    spec.environment(key, value)
                }
            }
            
            // https://developer.android.com/guide/practices/page-sizes#other-build-systems
            spec.environment("RUST_ANDROID_GRADLE_CC_LINK_ARG", "-Wl,-z,max-page-size=16384,-soname,lib$libname.so")
            spec.environment("RUST_ANDROID_GRADLE_LINKER_WRAPPER_PY", "$projectDir/$module/../linker-wrapper.py")
            spec.environment("RUST_ANDROID_GRADLE_TARGET", "target/${toolchain.target}/$profile/lib$libname.so")
            
            // NDK環境変数を設定
            val androidConfig = project.extensions.getByType(com.android.build.gradle.LibraryExtension::class.java)
            val ndkVersion = androidConfig.ndkVersion ?: "26.1.10909125" // デフォルトのNDKバージョン
            val ndkMajorVersion = ndkVersion.split(".")[0]
            spec.environment("CARGO_NDK_MAJOR_VERSION", ndkMajorVersion)
        }
    }
}

tasks.whenTaskAdded {
    when (name) {
        "mergeDebugJniLibFolders", "mergeReleaseJniLibFolders" -> dependsOn("cargoBuild")
    }
}

tasks.register<Exec>("cargoClean") {
    executable("cargo")     // cargo.cargoCommand
    args("clean")
    workingDir("$projectDir/${cargo.module}")
}
tasks.clean.dependsOn("cargoClean")

dependencies {
    api(project(":plugin"))
    api(libs.androidx.core.ktx)
    api(libs.androidx.lifecycle.livedata.core.ktx)
    api(libs.androidx.preference)
    api(libs.androidx.room.runtime)
    api(libs.androidx.work.multiprocess)
    api(libs.androidx.work.runtime.ktx)
    api(libs.dnsjava)
    api(libs.kotlinx.coroutines.android)
    api(libs.material)
    api(libs.timber)
    coreLibraryDesugaring(libs.desugar)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit.ktx)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.test.runner)
}
