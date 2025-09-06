// Top-level build file where you can add configuration options common to all sub-projects/modules.
// This file applies plugins to the project from the version catalog (libs.versions.toml).
// The actual plugin artifacts are resolved using repositories defined in settings.gradle.kts -> pluginManagement.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false // Relies on libs.versions.toml for correct version (1.5.8)
    alias(libs.plugins.dagger.hilt.android) apply false
    alias(libs.plugins.google.ksp) apply false
}
