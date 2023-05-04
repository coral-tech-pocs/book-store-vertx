import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.testing.logging.TestLogEvent.*
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin ("jvm") version "1.8.10"
  application
  id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "com.coral.bookstore"
version = "1.0.0-SNAPSHOT"

repositories {
  mavenCentral()
}

val vertxVersion = "4.4.1"
val junitJupiterVersion = "5.9.1"

val mainVerticleName = "com.coral.bookstore.MainVerticle"
val launcherClassName = "io.vertx.core.Launcher"

val watchForChange = "src/**/*"
val doOnChange = "${projectDir}/gradlew classes"

application {
  mainClass.set(launcherClassName)
}

dependencies {
  implementation(platform("io.vertx:vertx-stack-depchain:$vertxVersion"))
  implementation("io.vertx:vertx-core")
  implementation("io.vertx:vertx-web-validation")
  implementation("io.vertx:vertx-sql-client-templates")
  implementation("io.vertx:vertx-web")
  implementation("io.vertx:vertx-pg-client")
  implementation("io.vertx:vertx-jdbc-client")
  implementation("io.vertx:vertx-web-openapi")
  implementation("io.vertx:vertx-lang-kotlin")
  implementation("io.vertx:vertx-web-client")
  implementation("io.reactiverse:reactiverse-junit5-web-client:0.3.0")
  implementation(kotlin("stdlib"))
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.2")
  implementation("org.liquibase:liquibase-core:4.20.0")
  implementation("org.postgresql:postgresql:42.6.0")
  implementation("com.ongres.scram:client:2.1")
  testImplementation("io.vertx:vertx-unit")
  testImplementation("org.mockito.kotlin:mockito-kotlin:4.1.0")
  testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
  testImplementation("org.assertj:assertj-core:3.24.2")
  testImplementation("com.h2database:h2:2.1.214")




}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions.jvmTarget = "17"

tasks.withType<ShadowJar> {
  archiveClassifier.set("fat")
  manifest {
    attributes(mapOf("Main-Verticle" to mainVerticleName))
  }
  mergeServiceFiles()
}

tasks.withType<Test> {
  useJUnitPlatform()
  testLogging {
    events = setOf(PASSED, SKIPPED, FAILED)
    exceptionFormat = FULL
    showStandardStreams = true
  }
}

tasks.withType<JavaExec> {
  args = listOf("run", mainVerticleName, "--redeploy=$watchForChange", "--launcher-class=$launcherClassName", "--on-redeploy=$doOnChange")
}
