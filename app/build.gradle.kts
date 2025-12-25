plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    // id("com.google.gms.google-services") // Uncomment this line after adding google-services.json
}

android {
    namespace = "com.example.doan"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.doan"
        minSdk = 24
        targetSdk = 36
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

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    sourceSets {
        getByName("main") {
            res.srcDirs(
                "src/main/res",
                "src/main/res-user",
                "src/main/res-manager"
            )
        }
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.core.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    
    // FIX Medium #11: Thêm Mockito cho unit tests
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.22")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.9.0") // Added this line
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    
    // FIX C4: LocalBroadcastManager for token expired handling
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")
    // CircleIndicator for ViewPager2
    implementation("me.relex:circleindicator:2.1.6")
    
    // Google Maps & Location
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.1.0")

    implementation("com.google.android.libraries.places:places:3.4.0")

    // Compose
    val composeBom = platform("androidx.compose:compose-bom:2024.02.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.8.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("dev.chrisbanes.snapper:snapper:0.3.0")
    
    // MPAndroidChart for charts/graphs
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Biometric authentication
    implementation("androidx.biometric:biometric:1.1.0")

    // Phone Authentication
//    implementation("com.twilio:twilio-conversations:10.1.0")

    // Notification
    implementation("com.onesignal:OneSignal:[5.0.0, 5.99.99]")

    implementation ("com.airbnb.android:lottie:+")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")
}

tasks.register("deleteDuplicateResources") {
    doLast {
        val duplicateFile = file("src/main/res/layout/fragment_store.xml")
        if (duplicateFile.exists()) {
            println("Deleting duplicate file: ${duplicateFile.absolutePath}")
            duplicateFile.delete()
        }
        
        val duplicateDetailFile = file("src/main/res/layout/activity_product_detail.xml")
        if (duplicateDetailFile.exists()) {
            println("Deleting duplicate file: ${duplicateDetailFile.absolutePath}")
            duplicateDetailFile.delete()
        }
    }
}

// Hook into the build process
tasks.named("preBuild") {
    dependsOn("deleteDuplicateResources")
}

