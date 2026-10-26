import java.io.File

plugins {
    alias(libs.plugins.android.library)
    id("com.chaquo.python")
}

/**
 * Chaquopy's host Python must be 3.13 because the vendored Android
 * pydantic-core wheel is cp313. The frozen casparser sources are not modified.
 *
 * pypdfium2 5.13.0 publishes an Android arm64 wheel and no Android x86_64 wheel.
 * The emulator debug build stays x86_64 so the model probe can load. A phone
 * build sets GUD_PHONE_PARSER=1 and packages the real PDFium wheel.
 */
val phoneParser = System.getenv("GUD_PHONE_PARSER") == "1"

fun resolveHostPython313(): String {
    val fromEnv = System.getenv("GUD_BUILD_PYTHON")?.takeIf { it.isNotBlank() }
    if (fromEnv != null && File(fromEnv).canExecute()) return fromEnv
    val onPath = System.getenv("PATH").orEmpty().split(File.pathSeparator).firstNotNullOfOrNull { dir ->
        File(dir, "python3.13").takeIf { it.canExecute() }?.absolutePath
    }
    if (onPath != null) return onPath
    val uvRoot = File(System.getProperty("user.home"), ".local/share/uv/python")
    if (uvRoot.isDirectory) {
        uvRoot.walkTopDown().maxDepth(4)
            .firstOrNull { it.name == "python3.13" && it.canExecute() }
            ?.absolutePath
            ?.let { return it }
    }
    return "python3.13"
}

android {
    namespace = "in.growurdhan.casruntime"
    compileSdk = 36
    defaultConfig {
        minSdk = 26
        ndk { abiFilters += listOf(if (phoneParser) "arm64-v8a" else "x86_64") }
    }
}

chaquopy {
    defaultConfig {
        version = "3.13"
        buildPython(resolveHostPython313())
        pip {
            // Offline: the Android pydantic-core wheel is local, and PyPI has no
            // matching Android build of it. Pure-Python deps are vendored beside it.
            options("--no-index", "--find-links", projectDir.resolve("wheels").absolutePath)
            install("typing-extensions==4.16.0")
            install("typing-inspection==0.4.4")
            install("annotated-types==0.7.0")
            install("pydantic_core==2.50.1")
            install("pydantic==2.14.1")
            install("six==1.17.0")
            install("python-dateutil==2.9.0.post0")
            if (phoneParser) install("pypdfium2==5.13.0")
        }
    }
    sourceSets.getByName("main") {
        // Reference frozen source without copying or modifying it. The existing
        // black-box Python entrypoint's imports remain identical to legacy.
        srcDir("../../scripts")
        srcDir("../../src/casparser-main")
        srcDir("../app/src/main/python")
    }
}