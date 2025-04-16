plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    id("com.google.gms.google-services")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

android {
    namespace = "com.example.fdea"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.fdea"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation ("androidx.compose.runtime:runtime:<compose_version>")
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation(libs.androidx.material3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.material3.android)
    implementation("androidx.compose.material:material-icons-extended:1.1.1")
    implementation ("com.google.firebase:firebase-messaging")
    implementation("androidx.navigation:navigation-runtime-ktx:2.7.5")
    implementation("androidx.navigation:navigation-compose:2.5.3")
    implementation("androidx.navigation:navigation-runtime-ktx:2.7.5")
    implementation ("androidx.navigation:navigation-compose:2.7.5")
    implementation("io.coil-kt:coil-compose:2.4.0")
    implementation ("com.google.firebase:firebase-database")

    implementation ("com.google.accompanist:accompanist-insets:0.24.7-alpha")
    implementation ("androidx.compose.material3:material3:1.2.1")
    implementation ("androidx.compose.material3:material3-window-size-class:1.2.1")
    implementation ("androidx.compose.runtime:runtime-livedata:1.2.1")
    implementation ("androidx.compose.material3:material3-adaptive-navigation-suite:1.0.0-alpha05")
    implementation ("androidx.core:core-splashscreen:1.0.1")
    implementation ("androidx.compose.runtime:runtime-livedata:<compose_version>")
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.32.0")
    implementation("com.google.maps.android:maps-compose:2.11.4")
    implementation("com.google.android.gms:play-services-maps:18.1.0")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation ("androidx.compose.ui:ui:1.4.3" )
    implementation ("androidx.compose.material3:material3:1.0.0")
    implementation ("androidx.compose.material:material:1.4.3")
    implementation ("androidx.compose.ui:ui-tooling-preview:1.4.3")
    implementation ("androidx.compose.material:material-icons-core:1.4.3")
    //implementation ("androidx.compose.material:material-icons-extended:1.4.3")
    implementation("androidx.compose.material:material-icons-extended:1.5.0")

    implementation ("androidx.navigation:navigation-compose:2.5.3")

    implementation ("com.google.mlkit:text-recognition-korean:16.0.0")
    implementation ("com.google.android.gms:play-services-mlkit-text-recognition-korean:16.0.0")
    implementation(platform("com.google.firebase:firebase-bom:33.2.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.github.bumptech.glide:glide:4.13.0")
    implementation("com.google.code.gson:gson:2.8.6")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.6.0")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")
    implementation(libs.firebase.auth)
    implementation(libs.play.services.auth)
    implementation (libs.play.services.auth.v1700)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.firebase.database.ktx)
    implementation(libs.google.firebase.firestore.ktx)
    implementation(libs.firebase.storage.ktx)
    implementation("com.google.android.libraries.places:places:2.4.0")
    implementation ("com.android.volley:volley:1.2.1")
    implementation ("com.algolia:instantsearch-android:3.1.1")

    implementation(libs.material)
    implementation(libs.play.services.drive)
    annotationProcessor("com.github.bumptech.glide:compiler:4.13.0")
    implementation ("com.google.android.gms:play-services-auth:20.5.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    debugImplementation ("androidx.compose.ui:ui-tooling:1.4.3")

    // Retrofit 라이브러리
    implementation ("com.squareup.retrofit2:retrofit:2.6.4")
    // Gson 변환기 라이브러리
    implementation("com.squareup.retrofit2:converter-gson:2.6.4")
    implementation("com.squareup.retrofit2:converter-scalars:2.6.4")

}