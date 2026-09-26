import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(libs.org.json)
}

compose.desktop {
    application {
        mainClass = "com.bloomee.app.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Dmg, TargetFormat.Msi)
            packageName = "Bloomee"
            // Release CI overrides this from the git tag (-PversionName).
            packageVersion = providers.gradleProperty("versionName").orNull ?: "1.0.0"
            description = "Bloomee — regl, su ve kalori takibi (masaüstü)"
            vendor = "Eruldin"
        }
    }
}
