// Standalone Gradle build for the macOS port.
//
// This build lives entirely inside `desktop/` so that NOT A SINGLE upstream file
// (Kotlin or Gradle) has to change. Run it from the repo root with:
//
//     ./gradlew -p desktop run
//
// It reuses upstream's version catalog by path, so library versions stay in
// lock-step with the Android app.

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "stash-desktop"
