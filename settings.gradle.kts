rootProject.name = "jvmapi"
//include(":test")

pluginManagement {
    repositories {
        maven(url = "http://oss.jfrog.org/oss-release-local")
        gradlePluginPortal()
    }
}
buildCache {
    local<DirectoryBuildCache> {
        directory = File(rootDir, "build-cache")
        removeUnusedEntriesAfterDays = 30
    }
}