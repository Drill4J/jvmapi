plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.epam.drill.cross-compilation")
    `maven-publish`
}

val scriptUrl: String by extra

apply(from = "$scriptUrl/git-version.gradle.kts")

repositories {
    mavenLocal()
    apply(from = "$scriptUrl/maven-repo.gradle.kts")
    mavenCentral()
    jcenter()
}

fun jvmPaths(target: String) =
    run {
        val includeBase = file("src")
            .resolve("nativeInterop")
            .resolve("cinterop")
            .resolve(target)
        val includeAddition = when (target) {
            "linuxX64" -> includeBase.resolve("linux")
            "macosX64" -> includeBase.resolve("darwin")
            "mingwX64" -> includeBase.resolve("win32")
            else -> throw RuntimeException("We don't know the prefix for ${System.getProperty("os.name")} target")
        }
        arrayOf(includeBase, includeAddition)
    }

val drillLogger: String by extra

kotlin {

    crossCompilation {
        common {
            defaultSourceSet {
                dependsOn(sourceSets.named("commonMain").get())
                dependencies {
                    implementation("com.epam.drill.logger:logger:$drillLogger")
                }
            }
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



    sourceSets.all {
        languageSettings.useExperimentalAnnotation("kotlin.ExperimentalUnsignedTypes")
    }
    sourceSets {
        commonMain {
            dependencies {
                implementation(kotlin("stdlib-common"))
            }
        }
        jvm {
            compilations["main"].defaultSourceSet {
                dependencies {
                    implementation(kotlin("stdlib-jdk8"))
                }
            }
        }
    }
}
