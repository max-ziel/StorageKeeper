plugins {
    java
    jacoco
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
    id("io.swagger.core.v3.swagger-gradle-plugin") version "3.0.0"
}

group = "io.github.maxziel"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.2.0")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server-test")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.5.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-postgresql")
}

springBoot {
    buildInfo {
        excludes = setOf("time")
    }
}

// OCI image with Cloud Native Buildpacks: ./gradlew bootBuildImage (needs Docker, no Dockerfile).
// The CI publishes it to the GitHub Container Registry with --publishImage.
val imageRepository =
    providers
        .gradleProperty("imageRepository")
        .getOrElse("ghcr.io/max-ziel/storagekeeper")

tasks.bootBuildImage {
    imageName = "$imageRepository:${project.version}"
    // The CI adds the commit as an extra tag (-PimageTag=<sha>), so every pushed image can be found again.
    tags = listOf("$imageRepository:latest") +
        providers.gradleProperty("imageTag").map { listOf("$imageRepository:$it") }.getOrElse(emptyList())
    environment = mapOf("BP_JVM_VERSION" to "25")
    docker {
        publishRegistry {
            username = providers.environmentVariable("REGISTRY_USERNAME")
            password = providers.environmentVariable("REGISTRY_PASSWORD")
        }
    }
}

extra["jackson-2-bom.version"] = "2.21.8"

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
    }
}
