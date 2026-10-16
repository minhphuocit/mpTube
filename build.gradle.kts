// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.test") version "9.3.1" apply false
    id("androidx.benchmark") version "1.5.0" apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}