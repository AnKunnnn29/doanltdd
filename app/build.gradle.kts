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

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.22")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // CircleIndicator for ViewPager2
    implementation("me.relex:circleindicator:2.1.6")
    
    // Google Maps & Location
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.1.0")
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
