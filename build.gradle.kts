@file:Suppress("UNUSED_VARIABLE")

import org.apache.tools.ant.taskdefs.condition.Os
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetPreset

plugins {
    id("org.jetbrains.kotlin.multiplatform") version ("1.3.50")
    id("com.jfrog.bintray") version ("1.8.3")
    id("com.jfrog.artifactory") version ("4.9.8")
}

apply(from = "https://raw.githubusercontent.com/Drill4J/build-scripts/master/publish.gradle")

repositories {
    mavenCentral()
}
val presetName =
    when {
        Os.isFamily(Os.FAMILY_MAC) -> "macosX64"
        Os.isFamily(Os.FAMILY_UNIX) -> "linuxX64"
        Os.isFamily(Os.FAMILY_WINDOWS) -> "mingwX64"
        else -> throw RuntimeException("Target ${System.getProperty("os.name")} is not supported")
    }

fun jvmPaths(target: String) =
    run {
        val includeBase = file("native")
            .resolve("nativeInterop")
            .resolve("cinterop")
            .resolve(target)
        val includeAddition = when {
            target == "linuxX64" -> includeBase.resolve("linux")
            target == "macosX64" -> includeBase.resolve("darwin")
            target == "mingwX64" -> includeBase.resolve("win32")
            else -> throw RuntimeException("We don't know the prefix for ${System.getProperty("os.name")} target")
        }
        arrayOf(includeBase, includeAddition)
    }

val isDevMode = System.getProperty("idea.active") == "true"
kotlin {
    targets {
        if (isDevMode)
            createNativeTargetForCurrentOs("native") {
            }
        else {
            mingwX64("windowsX64")
            linuxX64("linuxX64")
            macosX64("macosX64")
        }

    }

    sourceSets {
        val commonNativeSs = maybeCreate("nativeMain")
        if (!isDevMode) {
            val windowsX64Main by getting { dependsOn(commonNativeSs) }
            val linuxX64Main by getting { dependsOn(commonNativeSs) }
            val macosX64Main by getting { dependsOn(commonNativeSs) }
        }
    }
    configure(sourceSets) {
        val srcDir = if (name.endsWith("Main")) "src" else "test"
        val platform = name.dropLast(4)
        kotlin.srcDir("$platform/$srcDir")

    }
    configure(targets) {
        val targetName = name

        val cm = compilations["main"]
        if (cm is KotlinNativeCompilation) {
            File(project.projectDir, "native/nativeInterop/cinterop").listFiles()?.filter { it.isFile }
                ?.forEach { file ->
                    val filePath = file.absolutePath
                    val name = file.nameWithoutExtension
                    val objects = cm.cinterops?.create(name)
                    objects?.defFile(filePath)
                    objects?.includeDirs(jvmPaths(this.preset?.name!!), "./native/nativeInterop/cinterop")
                }
        }
    }

    sourceSets.all {
        languageSettings.useExperimentalAnnotation("kotlin.ExperimentalUnsignedTypes")
    }
}

fun KotlinMultiplatformExtension.createNativeTargetForCurrentOs(
    name: String,
    config: KotlinNativeTarget.() -> Unit
) {
    val createTarget = (presets.getByName(presetName) as KotlinNativeTargetPreset).createTarget(name)
    targets.add(createTarget)
    config(createTarget)

}

fun KotlinNativeTarget.mainCompilation(configureAction: Action<KotlinNativeCompilation>) =
    compilations.getByName("main", configureAction as Action<in KotlinNativeCompilation>)