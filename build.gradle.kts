plugins {
    java
    jacoco
    id("org.springframework.boot") version "3.0.6"
    id("io.spring.dependency-management") version "1.1.0"
}

group = "faang.school"
version = "1.0"
java.sourceCompatibility = JavaVersion.VERSION_17

jacoco {
    toolVersion = "0.8.9"
    reportsDirectory.set(layout.buildDirectory.dir("customJacocoReportDir"))
}

repositories {
    mavenCentral()
}

dependencies {
    /**
     * Spring boot starters
     */
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.kafka:spring-kafka:3.0.12")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign:4.0.2")
    implementation("org.springdoc","springdoc-openapi-starter-webmvc-ui","2.0.2")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    /**
     * Amazon S3
     */
    implementation("com.amazonaws:aws-java-sdk-s3:1.12.481")

    /**
    * Database
     */
    implementation("org.liquibase:liquibase-core")
    implementation("redis.clients:jedis:4.3.2")
    runtimeOnly("org.postgresql:postgresql")
    implementation ("org.apache.kafka:kafka-clients:2.8.0")
    implementation ("org.springframework.kafka:spring-kafka:2.8.0")
    implementation ("org.apache.kafka:kafka-streams:2.8.0")

    /**
     * Utils & Logging
     */
    implementation("com.fasterxml.jackson.core:jackson-databind:2.14.2")
    implementation("org.slf4j:slf4j-api:2.0.5")
    implementation("ch.qos.logback:logback-classic:1.4.6")
    implementation("org.projectlombok:lombok:1.18.26")
    annotationProcessor("org.projectlombok:lombok:1.18.26")
    implementation("org.mapstruct:mapstruct:1.5.3.Final")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.5.3.Final")
    implementation ("com.google.guava:guava:32.1.2-jre")

    /**
     * Test containers
     */
    implementation(platform("org.testcontainers:testcontainers-bom:1.17.6"))
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("com.redis.testcontainers:testcontainers-redis-junit-jupiter:1.4.6")

    /**
     * Tests
     */
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.9.2")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

val test by tasks.getting(Test::class) { testLogging.showStandardStreams = true }

tasks.bootJar {
    archiveFileName.set("service.jar")
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}
tasks.jacocoTestReport {
    dependsOn(tasks.test)
}

tasks.jacocoTestReport {
    reports {
        xml.required.set(false)
        csv.required.set(false)
        html.outputLocation.set(layout.buildDirectory.dir("jacocoHtml"))
    }
    classDirectories.setFrom(files(classDirectories.files.map {
        fileTree(it).apply {
            exclude("faang/school/postservice/client/**",
                    "faang/school/postservice/config/**",
                    "faang/school/postservice/dto/**",
                    "faang/school/postservice/exception/**",
                    "faang/school/postservice/mapper/**",
                    "faang/school/postservice/model/**",
                    "faang/school/postservice/repository/**",
                    "faang/school/postservice/util/**",
                    "faang/school/postservice/PostServiceApp.class")
        }
    }))
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            element = "CLASS"
            excludes = listOf("faang.school.postservice.client.**",
                    "faang.school.postservice.config.**",
                    "faang.school.postservice.dto.**",
                    "faang.school.postservice.exception.**",
                    "faang.school.postservice.mapper.**",
                    "faang.school.postservice.model.**",
                    "faang.school.postservice.repository.**",
                    "faang.school.postservice.util.**",
                    "faang.school.postservice.PostServiceApp")
            limit {
                minimum = "0.8".toBigDecimal()
            }
        }
    }
}