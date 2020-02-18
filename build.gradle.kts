plugins {
    id("org.jetbrains.kotlin.multiplatform") version ("1.3.61")
    id("com.epam.drill.cross-compilation") version "0.15.1"
}

apply(from = "https://raw.githubusercontent.com/Drill4J/build-scripts/master/publish.gradle")

repositories {
    mavenCentral()
}

fun jvmPaths(target: String) =
    run {
        val includeBase = file("src")
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

kotlin {

    crossCompilation{
        common{
            cinterops.create("jvmti") { includeDirs(jvmPaths(target.preset?.name!!)) }
        }
    }
    setOf(
        mingwX64(),
        linuxX64(),
        macosX64()
    ).forEach { target ->
        target.compilations["main"].cinterops.create("jvmti") { includeDirs(jvmPaths(target.preset?.name!!)) }
    }
    jvm()


    sourceSets.all {
        languageSettings.useExperimentalAnnotation("kotlin.ExperimentalUnsignedTypes")
    }
}