// settings.gradle.kts
// IMPORTANT: The pluginManagement block MUST be the very first executable code in this file.
// No comments (other than this one if necessary) or any other statements should precede it.
pluginManagement {
    repositories {
        google()          // For Android Gradle Plugin and other Google-hosted plugins
        mavenCentral()    // For Kotlin, Jetpack Compose, and many other open-source plugins
        gradlePluginPortal() // For plugins published to the Gradle Plugin Portal
        // Ensure these repositories are accessible from your network environment.
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // You can add other repositories here if needed, e.g., for specific libraries
        // maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "DroidCalc"
include(":app")
