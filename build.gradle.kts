import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.internal.jvm.Jvm
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetPreset

plugins {
    id("org.jetbrains.kotlin.multiplatform") version ("1.3.30")
}
repositories {
    mavenCentral()
}
val preset =
        when {
            Os.isFamily(Os.FAMILY_MAC) -> "macosX64"
            Os.isFamily(Os.FAMILY_UNIX) -> "linuxX64"
            Os.isFamily(Os.FAMILY_WINDOWS) -> "mingwX64"
            else -> throw RuntimeException("Target ${System.getProperty("os.name")} is not supported")
        }

val jvmPaths =
        Jvm.current().javaHome.toPath().run {
            val includeBase = this.resolve("include")

            val includeAddition = when {
                Os.isFamily(Os.FAMILY_UNIX) -> includeBase.resolve("linux")
                Os.isFamily(Os.FAMILY_MAC) -> includeBase.resolve("darwin")
                Os.isFamily(Os.FAMILY_WINDOWS) -> includeBase.resolve("win32")
                else -> throw RuntimeException("We don't know the prefix for ${System.getProperty("os.name")} target")
            }
            arrayOf(includeBase, includeAddition)
        }

kotlin {
    targets {
        createNativeTargetForCurrentOs("jvmapi") {
            mainCompilation {
                val jvmapi by cinterops.creating
                jvmapi.apply {
                    includeDirs(jvmPaths,"./src/nativeInterop/cinterop")
                }
            }
        }
    }

    sourceSets.all {
        languageSettings.useExperimentalAnnotation("kotlin.ExperimentalUnsignedTypes")
    }
}

fun KotlinMultiplatformExtension.createNativeTargetForCurrentOs(
        name: String,
        config: KotlinNativeTarget.() -> Unit = {}
) {
    val createTarget = (presets.getByName(preset) as KotlinNativeTargetPreset).createTarget(name)
    targets.add(createTarget)
    config(createTarget)
}

fun KotlinNativeTarget.mainCompilation(configureAction: Action<KotlinNativeCompilation>) {
    compilations.getByName("main", configureAction as Action<in KotlinNativeCompilation>)
}