plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.devtools.ksp)
    alias(libs.plugins.navigation.safeargs)
    alias(libs.plugins.google.services)
    alias(libs.plugins.kotlin.serialization)
}
android {
    namespace = "com.mynikatech.apnafund"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mynikatech.apnafund"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf(
                    "room.schemaLocation" to "$projectDir/schemas"
                )
            }
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    buildTypes {
        debug {
            // Emulator → host
            //buildConfigField("String", "SERVER_BASE_URL", "\"http://10.0.2.2:8080\"")
            //buildConfigField("String", "SERVER_BASE_URL", "\"https://10.0.2.2:8443\"")
            buildConfigField("String", "SERVER_BASE_URL", "\"https://api-dev.apnafund.mynikatech.in\"")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            //buildConfigField("String", "SERVER_BASE_URL", "\"http://10.0.2.2:8080\"")
            //buildConfigField("String", "SERVER_BASE_URL", "\"https://10.0.2.2:8443\"")
            buildConfigField("String", "SERVER_BASE_URL", "\"https://api-dev.apnafund.mynikatech.in\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.fragment.ktx.v187)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.viewmodel.lifecycle)
    implementation(libs.androidx.constraintlayout)

    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.auth)
    implementation(libs.google.play.services.auth)


    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.cardview)
    implementation(libs.places)
    implementation(libs.firebase.storage)
    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation(libs.firebase.auth)

    // If this project uses any Kotlin source, use Kotlin Symbol Processing (KSP)
    // See Add the KSP plugin to your project
    ksp(libs.room.compiler)

    // If this project only uses Java source, use the Java annotationProcessor
    // No additional plugins are necessary
    annotationProcessor(libs.room.compiler)

    // optional - Kotlin Extensions and Coroutines support for Room
    implementation(libs.androidx.room.ktx)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.arch.core.testing)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.google.play.services.auth)
    implementation(libs.amplify)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)
    implementation(project(":api"))

    components {
        withModule("androidx.emoji2:emoji2") {
            allVariants {
                withDependencies {
                    removeIf { it.group == "androidx.emoji2" }
                }
            }
        }
        withModule("androidx.emoji2:emoji2-views-helper") {
            allVariants {
                withDependencies {
                    removeIf { it.group == "androidx.emoji2" }
                }
            }
        }
    }
}
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
    arg("room.generateKotlin", "true")
}