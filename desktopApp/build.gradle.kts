plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.compose.runtime:runtime:1.11.1")
    implementation("org.jetbrains.compose.foundation:foundation:1.11.1")
    implementation("org.jetbrains.compose.ui:ui:1.11.1")
    implementation("org.jetbrains.compose.material3:material3:1.9.0")
    implementation("org.jetbrains.compose.ui:ui-tooling-preview:1.11.1")
    implementation(libs.kotlinx.coroutinesSwing)
    implementation(libs.kotlinxSerializationJson)
    implementation("org.holypresenter:platform-api:0.6.0")
    implementation("org.holypresenter:platform-ui:0.7.1") {
        exclude(group = "androidx.compose.material3")
        exclude(group = "androidx.compose.runtime")
        exclude(group = "androidx.compose.foundation")
        exclude(group = "androidx.compose.ui")
    }
}

tasks.withType<Jar>().configureEach {
    archiveBaseName.set("bible")
    archiveVersion.set("")
}

val holyPresenterModulesDir =
    file(
        "D:/Idea/HolyPresenter/desktopApp/modules"
    )

val cleanOldPlatformUi by
tasks.registering(Delete::class) {

    delete(
        fileTree(holyPresenterModulesDir) {
            include("platform-ui-*.jar")
        }
    )
}

val installModule by
tasks.registering(Copy::class) {
    description = "Copies the Bible module into HolyPresenter"
    dependsOn(cleanOldPlatformUi)

    from(
        tasks.named<Jar>("jar")
            .flatMap {
                it.archiveFile
            }
    )

    from(configurations.runtimeClasspath) {
        include("platform-ui-*.jar")
    }

    into(holyPresenterModulesDir)
}

tasks.named<Jar>("jar") {
    finalizedBy(installModule)
}