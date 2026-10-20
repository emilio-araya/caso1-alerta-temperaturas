plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.caso1"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.caso1"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // API del sistema de monitoreo. Por defecto: json-server en el PC, alcanzado con
        // `adb reverse tcp:3000 tcp:3000` (emulador o celular por USB). Alternativa en emulador:
        // -Pcaso1.apiUrl=http://10.0.2.2:3000/api/v1/ . Con caso1.apiSimulada=true la app usa
        // el servidor simulado interno y no necesita el PC. Ver mock-api/README.md.
        val apiUrl = providers.gradleProperty("caso1.apiUrl").getOrElse("http://localhost:3000/api/v1/")
        // Retrofit rejects a base URL without the trailing slash at runtime: fail the build instead
        require(apiUrl.matches(Regex("^https?://.+/$"))) {
            "caso1.apiUrl must be an http(s) URL ending with '/', got: $apiUrl"
        }
        val apiSimulada = providers.gradleProperty("caso1.apiSimulada").getOrElse("false").toBoolean()
        buildConfigField("String", "API_URL_BASE", "\"$apiUrl\"")
        buildConfigField("boolean", "API_SIMULADA", apiSimulada.toString())
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
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
    testOptions {
        // android.util.Log (used by GalponRepository) returns defaults in JVM tests instead of throwing
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.window.size)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.datastore.core)
    implementation(libs.androidx.work)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
}
