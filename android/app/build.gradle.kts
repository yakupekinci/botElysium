plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.arxes.elysium"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.arxes.elysium"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
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
    kotlinOptions {
        jvmTarget = "17"
    }

    sourceSets {
        getByName("main").assets.srcDir(layout.buildDirectory.dir("generated/assets"))
    }
}

val prepareWebAssets by tasks.registering(Copy::class) {
    from("${rootDir.parentFile.absolutePath}/index.html")
    from("${rootDir.parentFile.absolutePath}/sw.js")
    from("${rootDir.parentFile.absolutePath}/manifest.webmanifest")
    from("${rootDir.parentFile.absolutePath}/icon.svg")
    into(layout.buildDirectory.dir("generated/assets"))
}

tasks.matching { it.name == "preBuild" }.configureEach {
    dependsOn(prepareWebAssets)
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
}
