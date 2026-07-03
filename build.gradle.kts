plugins {
    java
    id("org.springframework.boot") version "3.4.2"
    id("io.spring.dependency-management") version "1.1.7"
    jacoco
    // Protobuf плагин — генерирует Java-клиентский код из telemetry.proto
    id("com.google.protobuf") version "0.9.4"
}

group = "seminars"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

springBoot {
    mainClass.set("seminars.Main")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.5")
    implementation("org.flywaydb:flyway-database-postgresql")

    runtimeOnly("org.postgresql:postgresql")

    // Kafka — асинхронный обмен событиями о спутниках между микросервисами
    implementation("org.springframework.kafka:spring-kafka")

    // gRPC Client — подключается к satellite-telemetry на порту 9091
    implementation("net.devh:grpc-client-spring-boot-starter:3.1.0.RELEASE")
    implementation("net.devh:grpc-server-spring-boot-starter:3.1.0.RELEASE")
    implementation("io.grpc:grpc-protobuf:1.62.2")
    implementation("io.grpc:grpc-stub:1.62.2")
    compileOnly("org.apache.tomcat:annotations-api:6.0.53")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("com.h2database:h2")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
}

// Protobuf для генерации gRPC-клиентских стабов из telemetry.proto
protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.25.1"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.62.2"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                create("grpc")
            }
        }
    }
}

sourceSets {
    main {
        java {
            srcDirs(
                "build/generated/source/proto/main/java",
                "build/generated/source/proto/main/grpc"
            )
        }
    }
}

jacoco {
    toolVersion = "0.8.12"
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    // grpc-server-spring-boot-starter есть в общем classpath модуля и по умолчанию
    // автоматически поднимает embedded gRPC-сервер для ЛЮБОГО Spring Boot приложения,
    // где он есть на classpath — даже без единого @GrpcService бина (component scan
    // seminars.Main не заходит в пакет telemetry.service, так что своих gRPC-сервисов
    // у Main нет). Main нужен только gRPC-КЛИЕНТ (TelemetryGrpcClient), поэтому
    // встроенный сервер явно отключаем: grpc.server.port=-1 — официальный способ
    // отключения из grpc-spring-boot-starter. Без этого Main пытается бы занять тот же
    // порт 9091, что и реальный сервер telemetry.TelemetryApplication → BindException.
    jvmArgs = listOf("-Dfile.encoding=UTF-8", "-Dconsole.encoding=UTF-8", "-Dgrpc.server.port=-1")
}

// Проект собирает три отдельных Spring Boot приложения в одном модуле
// (seminars.Main, telemetry.TelemetryApplication, scheduler.SchedulerApplication),
// но springBoot.mainClass указывает только на seminars.Main, поэтому обычный
// `./gradlew bootRun` поднимает только REST API. Для локальной разработки без
// Docker gRPC-сервер телеметрии и планировщик нужно запускать этими тасками
// отдельными процессами (в отдельных терминалах), например:
//   ./gradlew runTelemetry
//   ./gradlew runScheduler
tasks.register<JavaExec>("runTelemetry") {
    group = "application"
    description = "Запускает gRPC-сервер телеметрии (telemetry.TelemetryApplication, порт 9091)"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("telemetry.TelemetryApplication")
    jvmArgs = listOf("-Dfile.encoding=UTF-8", "-Dconsole.encoding=UTF-8")
}

tasks.register<JavaExec>("runScheduler") {
    group = "application"
    description = "Запускает планировщик миссий (scheduler.SchedulerApplication, порт 8081)"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("scheduler.SchedulerApplication")
    // Та же причина, что и у bootRun выше: у scheduler тоже нет ни одного @GrpcService,
    // но встроенный gRPC-сервер стартовал бы всё равно и занял бы порт 9091,
    // конфликтуя с telemetry.TelemetryApplication.
    jvmArgs = listOf("-Dfile.encoding=UTF-8", "-Dconsole.encoding=UTF-8", "-Dgrpc.server.port=-1")
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        html.required.set(true)
        xml.required.set(true)
    }
}
