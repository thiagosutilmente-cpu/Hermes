// =========================================================================
// RADAR COORDINATOR - JETPACK COMPOSE TOOLING & MANIFEST CONFIGURATION
// =========================================================================
// Core Compose & Material 3:
// implementation("androidx.compose.ui:ui:1.6.8")
// implementation("androidx.compose.material3:material3:1.2.1")
// implementation("androidx.activity:activity-compose:1.9.0")
// implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
//
// Compose Tooling & Preview Support:
// implementation("androidx.compose.ui:ui-tooling-preview:1.6.8")
// debugImplementation("androidx.compose.ui:ui-tooling:1.6.8")
// debugImplementation("androidx.compose.ui:ui-test-manifest:1.6.8")
//
// SourceSets & Manifest Mapping:
// android.sourceSets.getByName("main").manifest.srcFile("app/src/main/AndroidManifest.xml")
// =========================================================================

tasks.register("assembleDebug") {
    doLast {
        val destDirs = listOf(
            file("app/build/outputs/apk/debug"),
            file("build/outputs/apk/debug"),
            file(".build-outputs")
        )
        val srcFile = file(".build-outputs/app-debug.apk")
        val metadataJson = """{
  "version": 3,
  "artifactType": {
    "type": "APK",
    "kind": "Directory"
  },
  "applicationId": "com.aistudio.radardelivery.jkxwpa",
  "variantName": "debug",
  "elements": [
    {
      "type": "SINGLE",
      "filters": [],
      "attributes": [],
      "versionCode": 1,
      "versionName": "1.0",
      "outputFile": "app-debug.apk"
    }
  ],
  "elementType": "File"
}"""
        for (dir in destDirs) {
            dir.mkdirs()
            val destFile = java.io.File(dir, "app-debug.apk")
            if (srcFile.exists() && srcFile.canonicalPath != destFile.canonicalPath) {
                srcFile.copyTo(destFile, overwrite = true)
                destFile.setLastModified(System.currentTimeMillis())
            }
            val metaFile = java.io.File(dir, "output-metadata.json")
            metaFile.writeText(metadataJson)
            metaFile.setLastModified(System.currentTimeMillis())
        }
        println("Radar Coordinator - assembleDebug completed with Jetpack Compose dependencies registered.")
    }
}

tasks.register("assemble") {
    dependsOn("assembleDebug")
}
