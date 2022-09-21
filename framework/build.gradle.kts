import org.jetbrains.compose.compose

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }
        withJava()
    }
    sourceSets {
        val jvmMain by getting {
            dependencies {
                api(compose.desktop.currentOs)

                implementation(libs.lwjgl)
                implementation(libs.lwjgl.glfw)
                implementation(libs.lwjgl.opengl)
                implementation(libs.lwjgl.openvr)

                runtimeOnly(dependencies.variantOf(libs.lwjgl) {
                    classifier("natives-windows")
                })
                runtimeOnly(dependencies.variantOf(libs.lwjgl.glfw) {
                    classifier("natives-windows")
                })
                runtimeOnly(dependencies.variantOf(libs.lwjgl.opengl) {
                    classifier("natives-windows")
                })
                runtimeOnly(dependencies.variantOf(libs.lwjgl.openvr) {
                    classifier("natives-windows")
                })
            }
        }
        val jvmTest by getting
    }
}
