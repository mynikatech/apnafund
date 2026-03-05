plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktor.io)
}

dependencies {
    // Ktor
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.hsts)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.forwarded.header)
    implementation("io.getstream:stream-chat-java:1.37.0")
// Firebase Admin
    implementation("com.google.firebase:firebase-admin:9.2.0")

    // Logging
    implementation(libs.logback.classic)

    // DB stack
    implementation(libs.postgresql)
    implementation(libs.hikari)
    implementation(libs.flyway.core)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.dao)
    implementation(libs.jdbi.core)
    implementation(libs.jdbi.kotlin)
    implementation(libs.jdbi.sqlobject)
    implementation(libs.jdbi.kotlin.sqlobject)
    implementation(libs.ktor.server.call.id)
    implementation(libs.ktor.server.status.pages)
    testImplementation(kotlin("test"))
    implementation(project(":api"))

    implementation(libs.aws.sns)
    implementation(libs.aws.sqs)
    implementation(libs.aws.ses)

}

kotlin {
    // Use a modern JDK; 21 is fine and matches Studio’s JBR
    jvmToolchain(21)

}
application { mainClass.set("com.mynikatech.apnafund.server.ApplicationKt")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=true")}

tasks.shadowJar {
    archiveFileName.set("apnafund-server.jar")
    mergeServiceFiles() // 🔑 REQUIRED FOR gRPC
}