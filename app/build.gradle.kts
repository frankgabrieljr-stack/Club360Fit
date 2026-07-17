import groovy.json.JsonSlurper
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.org.jetbrains.kotlin.android)
    alias(libs.plugins.org.jetbrains.kotlin.plugin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val localProperties = Properties()
val localPropertiesFile: File = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { 
        localProperties.load(it) 
    }
}

val googleServicesFile: File = project.file("google-services.json")
val googleServicesJson = if (googleServicesFile.exists()) {
    @Suppress("UNCHECKED_CAST")
    JsonSlurper().parse(googleServicesFile) as Map<String, Any?>
} else {
    emptyMap()
}

val googleServicesProjectInfo = googleServicesJson["project_info"] as? Map<*, *>
val googleServicesClient = (googleServicesJson["client"] as? List<*>)
    ?.mapNotNull { it as? Map<*, *> }
    ?.firstOrNull { client ->
        val clientInfo = client["client_info"] as? Map<*, *>
        val androidInfo = clientInfo?.get("android_client_info") as? Map<*, *>
        androidInfo?.get("package_name") == "com.club360fit.app"
    }
val googleServicesApiKey = ((googleServicesClient?.get("api_key") as? List<*>)
    ?.firstOrNull() as? Map<*, *>)
    ?.get("current_key") as? String
val googleServicesAndroidAppId = (googleServicesClient?.get("client_info") as? Map<*, *>)
    ?.get("mobilesdk_app_id") as? String

fun localProperty(name: String): String? =
    localProperties.getProperty(name)?.trim()?.takeIf { it.isNotEmpty() }

android {
    namespace = "com.club360fit.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.club360fit.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 11
        versionName = "1.7.0"

        // Must match iOS AppConfig.supabaseURL and Supabase Dashboard → Settings → API → Project URL.
        // Wrong ref fails DNS ("Unable to resolve host"). Correct ref ends with …ahxtxvxq (not …ahxtvxvq).
        // Note: empty SUPABASE_URL= in local.properties yields "" (not null) — treat as missing.
        val supabaseUrl =
            localProperties.getProperty("SUPABASE_URL")?.trim()?.takeIf { it.isNotEmpty() }
                ?: "https://mjkrokpctcieahxtxvxq.supabase.co"
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")

        // Supabase anon key: add SUPABASE_ANON_KEY=your_key to project root local.properties
        val anonKey = localProperties.getProperty("SUPABASE_ANON_KEY") ?: ""
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$anonKey\"")

        // Prefer google-services.json for Firebase app settings. local.properties remains a fallback.
        val firebaseProjectId =
            (googleServicesProjectInfo?.get("project_id") as? String) ?: localProperty("FIREBASE_PROJECT_ID").orEmpty()
        val firebaseAndroidAppId = googleServicesAndroidAppId ?: localProperty("FIREBASE_ANDROID_APP_ID").orEmpty()
        val firebaseApiKey = googleServicesApiKey ?: localProperty("FIREBASE_API_KEY").orEmpty()
        val firebaseSenderId =
            (googleServicesProjectInfo?.get("project_number") as? String) ?: localProperty("FIREBASE_SENDER_ID").orEmpty()
        buildConfigField("String", "FIREBASE_PROJECT_ID", "\"$firebaseProjectId\"")
        buildConfigField("String", "FIREBASE_ANDROID_APP_ID", "\"$firebaseAndroidAppId\"")
        buildConfigField("String", "FIREBASE_API_KEY", "\"$firebaseApiKey\"")
        buildConfigField("String", "FIREBASE_SENDER_ID", "\"$firebaseSenderId\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    debugImplementation(libs.androidx.ui.tooling)

    // Supabase
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.gotrue)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.functions)
    implementation(libs.supabase.storage)
    implementation(libs.ktor.client.android)
    implementation(libs.coil.compose)
    implementation(libs.kotlinx.datetime)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.zxing.core)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
}
