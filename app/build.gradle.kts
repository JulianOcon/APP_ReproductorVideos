plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-kapt")
}

android {
    namespace = "com.example.reproductorvideos"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.reproductorvideos"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Dependencias básicas de Jetpack Compose
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-ui:1.3.1")
    implementation ("androidx.media3:media3-session:1.3.1")
    implementation ("com.github.bumptech.glide:glide:4.16.0")
    implementation(libs.material)
    annotationProcessor ("com.github.bumptech.glide:compiler:4.16.0")

    implementation ("androidx.palette:palette:1.0.0")


    implementation ("androidx.constraintlayout:constraintlayout:2.1.4")

    implementation  ("com.github.bumptech.glide:glide:4.15.1")
    implementation  ("jp.wasabeef:glide-transformations:4.3.0")
    annotationProcessor  ("com.github.bumptech.glide:compiler:4.15.1")


    // Dependencias de Retrofit para consumir la API
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.androidx.cardview)


    // Dependencias de testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    // Dependencias de debug
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.recyclerview)

    implementation(libs.volley)

    // Glide para cargar imágenes (miniaturas de videos)
    implementation(libs.github.glide)
    implementation(libs.wasabeef.glide.transformations)
    kapt("com.github.bumptech.glide:compiler:4.15.1")
    implementation (libs.androidx.localbroadcastmanager)
    
    
    // Glide para pantalla completa
    implementation (libs.androidx.media3.exoplayer.v111)
    implementation (libs.androidx.media3.ui.v111)

    implementation (libs.media3.ui.v131)




}

