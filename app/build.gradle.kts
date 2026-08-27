import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

val versionPropsFile = rootProject.file("version.properties")
val versionProps = Properties().apply {
    if (versionPropsFile.exists()) {
        FileInputStream(versionPropsFile).use { load(it) }
    } else {
        setProperty("VERSION_MAJOR", "0")
        setProperty("VERSION_MINOR", "8")
        setProperty("VERSION_PATCH", "27")
        setProperty("VERSION_BUILD", "0")
    }
}

val vMajor = (versionProps.getProperty("VERSION_MAJOR") ?: "0").toInt()
val vMinor = (versionProps.getProperty("VERSION_MINOR") ?: "8").toInt()
val vPatch = (versionProps.getProperty("VERSION_PATCH") ?: "27").toInt()
val vBuild = (versionProps.getProperty("VERSION_BUILD") ?: "0").toInt()

val appVersionCode = vMajor * 1000000 + vMinor * 10000 + vPatch * 100 + vBuild
val appVersionName = if (vBuild > 0) "$vMajor.$vMinor.$vPatch.$vBuild" else "$vMajor.$vMinor.$vPatch"

android {
  namespace = "com.example"
  compileSdk = 35

  defaultConfig {
    applicationId = "com.aistudio.tigerswidget.kyvazm"
    minSdk = 24
    targetSdk = 35
    versionCode = appVersionCode
    versionName = appVersionName

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
    debug {
      isDebuggable = true
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

tasks.register("packageVersionedApk") {
  description = "Packages and copies versioned APKs to build/outputs/apk/versioned/"
  dependsOn("assembleDebug")
  val vName = appVersionName
  val debugDir = layout.buildDirectory.dir("outputs/apk/debug")
  val versionedDir = layout.buildDirectory.dir("outputs/apk/versioned")
  doLast {
    val src = debugDir.get().file("app-debug.apk").asFile
    val destDir = versionedDir.get().asFile
    if (!destDir.exists()) destDir.mkdirs()
    if (src.exists()) {
      val dest = File(destDir, "Tigers-Widget-v${vName}.apk")
      src.copyTo(dest, overwrite = true)
      println("Created versioned APK: ${dest.absolutePath}")
    }
  }
}

tasks.register("printVersion") {
  val vName = appVersionName
  val vCode = appVersionCode
  doLast {
    println("Version Name: $vName")
    println("Version Code: $vCode")
  }
}

tasks.register("bumpVersion") {
  description = "Bumps the build number (e.g. 0.8.27 -> 0.8.27.1) in version.properties"
  val file = versionPropsFile
  doLast {
    val props = Properties()
    if (file.exists()) {
      FileInputStream(file).use { props.load(it) }
    }
    val major = (props.getProperty("VERSION_MAJOR") ?: "0").toInt()
    val minor = (props.getProperty("VERSION_MINOR") ?: "8").toInt()
    val patch = (props.getProperty("VERSION_PATCH") ?: "27").toInt()
    val currentBuild = (props.getProperty("VERSION_BUILD") ?: "0").toInt()
    val nextBuild = currentBuild + 1
    props.setProperty("VERSION_BUILD", nextBuild.toString())
    FileOutputStream(file).use { props.store(it, "Detroit Tigers Widget Version") }
    println("Updated version: $major.$minor.$patch.$nextBuild")
  }
}

tasks.register("bumpPatch") {
  description = "Bumps the patch version (e.g. 0.8.27 -> 0.8.28) in version.properties"
  val file = versionPropsFile
  doLast {
    val props = Properties()
    if (file.exists()) {
      FileInputStream(file).use { props.load(it) }
    }
    val major = (props.getProperty("VERSION_MAJOR") ?: "0").toInt()
    val minor = (props.getProperty("VERSION_MINOR") ?: "8").toInt()
    val currentPatch = (props.getProperty("VERSION_PATCH") ?: "27").toInt()
    val nextPatch = currentPatch + 1
    props.setProperty("VERSION_PATCH", nextPatch.toString())
    props.setProperty("VERSION_BUILD", "0")
    FileOutputStream(file).use { props.store(it, "Detroit Tigers Widget Version") }
    println("Updated version: $major.$minor.$nextPatch")
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

googleServices {
  missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN
}


// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  // implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.ai)
  implementation(libs.firebase.appcheck.recaptcha)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}
