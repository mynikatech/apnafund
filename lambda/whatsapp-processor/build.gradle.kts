plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
}

dependencies {
    implementation("com.amazonaws:aws-lambda-java-core:1.2.3")
    implementation("com.amazonaws:aws-lambda-java-events:3.11.4")
    implementation(project(":api"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation(libs.aws.sqs)
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation(libs.logback.classic)
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith("jar") }
            .map { zipTree(it) }
    })

    manifest {
        attributes["Main-Class"] =
            "com.mynikatech.apnafund.lambda.whatsapp.WhatsAppProcessorHandler"
    }

    archiveFileName.set("whatsapp-processor.jar")
}
