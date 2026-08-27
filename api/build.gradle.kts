plugins {
    // Compilador de Kotlin para JVM
    kotlin("jvm") version "2.3.21"

    // Hace que las clases sean abiertas automáticamente (necesario para proxies de Spring)
    kotlin("plugin.spring") version "2.3.21"

    // Plugin principal de Spring Boot (empaquetado, bootRun, etc.)
    id("org.springframework.boot") version "4.1.0"

    // Gestiona versiones de dependencias de Spring automáticamente
    id("io.spring.dependency-management") version "1.1.7"

    // Hace que las entidades JPA sean open (necesario para Hibernate + Kotlin)
    kotlin("plugin.jpa") version "2.3.21"

    // ========== Build Tools ==========

    // Formateador + linter de estilo
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"

    // Análisis estático de código (code smells, complejidad, bugs)
    id("dev.detekt") version "2.0.0-alpha.3"
}

group = "edu.austral.splitit"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // ========== Spring ==========

    // Necesario para que Spring funcione bien con Kotlin (data classes, etc.)
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Persistencia con JPA + Hibernate
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // Validaciones (Jakarta Validation)
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Web MVC (controllers, REST, etc.)
    implementation("org.springframework.boot:spring-boot-starter-webmvc")

    // Spring Security (JWT se firma con jjwt; BCrypt viene en este starter)
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-orgjson:0.12.6")

    // Hot reload en desarrollo
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // BOM para manejar versiones de dotenv
    // (x) - implementation(platform("me.paulschwarz:spring-dotenv-bom:5.1.0"))
    developmentOnly(platform("me.paulschwarz:spring-dotenv-bom:5.1.0"))

    // Carga variables desde archivo .env (solo en desarrollo)
    developmentOnly("me.paulschwarz:springboot4-dotenv")

    // Soporte de Jackson para clases Kotlin
    implementation("tools.jackson.module:jackson-module-kotlin")

    // Driver de PostgreSQL
    runtimeOnly("org.postgresql:postgresql")

    // ========== Test ==========
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Configuración de ktlint
ktlint {
    version.set("1.5.0")
    android.set(false)
    ignoreFailures.set(false)
    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
    }
}

// Configuración de detekt
detekt {
    buildUponDefaultConfig = true
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    jvmArgs = listOf("-Duser.timezone=America/Argentina/Buenos_Aires")
}
