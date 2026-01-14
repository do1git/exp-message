plugins {
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "1.9.25"
	id("org.springframework.boot") version "3.5.9"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "site.rahoon.message"
version = "0.0.1-SNAPSHOT"
description = "Demo project for Spring Boot"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-jdbc")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-data-redis")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-aop")
	implementation("org.springframework:spring-tx")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")
	implementation("io.jsonwebtoken:jjwt-api:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	runtimeOnly("com.mysql:mysql-connector-j")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("io.mockk:mockk:1.13.13")
	testImplementation("org.testcontainers:testcontainers:1.19.8")
	testImplementation("org.testcontainers:junit-jupiter:1.19.8")
	testImplementation("org.testcontainers:mysql:1.19.8")
	testImplementation("com.redis:testcontainers-redis:2.2.2")
	testImplementation("com.github.gavlyukovskiy:p6spy-spring-boot-starter:1.9.2")
	testImplementation("io.kotest:kotest-assertions-core:5.9.1")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

tasks.withType<Test> {
	useJUnitPlatform {
		// 태그 기반 필터링 (gradle -Ptest.tags=unit)
		val testTags = project.findProperty("test.tags") as String?
		if (testTags != null) {
			includeTags(testTags)
		}
	}
}

// 단위 테스트만 실행: ./gradlew unitTest
// @IntegrationTest가 없는 모든 테스트
tasks.register<Test>("unitTest") {
	description = "Run unit tests only (tests without @IntegrationTest)"
	group = "verification"
	useJUnitPlatform {
		excludeTags("integration")
	}
}

// 통합 테스트만 실행: ./gradlew integrationTest
// @IntegrationTest가 붙은 테스트만
tasks.register<Test>("integrationTest") {
	description = "Run integration tests only (tests with @IntegrationTest)"
	group = "verification"
	useJUnitPlatform {
		includeTags("integration")
	}
}

tasks.bootRun {
	val dotEnv = file(".env")
	if (dotEnv.exists()) {
		dotEnv.readLines()
			.filter { it.isNotBlank() && !it.startsWith("#") }
			.forEach { line ->
				val parts = line.split("=", limit = 2)
				if (parts.size == 2) {
					val key = parts[0].trim()
					val value = parts[1].trim()
					environment(key, value)
				}
			}
	}
}