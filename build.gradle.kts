plugins {
    java
    id("org.springframework.boot") version "3.4.2"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "scheduler"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

dependencies {
    // Web нужен для RestClient (HTTP-клиент, работающий с внешним сервисом)
    implementation("org.springframework.boot:spring-boot-starter-web")
    // Kafka — слушаем события о создании/удалении спутников
    implementation("org.springframework.kafka:spring-kafka")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
