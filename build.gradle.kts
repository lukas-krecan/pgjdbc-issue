import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.20"
    kotlin("plugin.spring") version "1.7.20"
    id("org.springframework.boot") version "2.7.4"
    id("io.spring.dependency-management") version "1.0.14.RELEASE"
    id("org.owasp.dependencycheck") version "7.2.1"
    id("com.github.ben-manes.versions") version "0.42.0"
    id("nu.studer.jooq") version "7.1.1"
    id("org.liquibase.gradle") version "2.1.1"
}

// specifies both compatibility version for Java sources and target JVM bytecode version
val javaVersion = JavaVersion.VERSION_17

val jooqVersion = "3.17.4"

group = "com.example"
version = "0.0.0"
java.sourceCompatibility = javaVersion

repositories {
    mavenCentral { }
}

dependencies {
    /* Kotlin */
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    /* Spring */
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-web") // We need this to expose actuator endpoints
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    /* Logging & monitoring */
    implementation("io.github.microutils:kotlin-logging:3.0.2")
    implementation("net.logstash.logback:logstash-logback-encoder:7.2")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.micrometer:micrometer-registry-datadog")
    implementation("io.github.mweirauch:micrometer-jvm-extras:0.2.2")
    implementation("io.sentry:sentry-logback:6.5.0")
    /* DB */
    implementation("org.liquibase:liquibase-core")
    implementation("org.postgresql:postgresql")
    implementation("org.jooq:jooq:$jooqVersion")
    implementation("org.jooq:jooq-kotlin:$jooqVersion")

    /* JOOQ generator */
    jooqGenerator("org.postgresql:postgresql")
    liquibaseRuntime("org.liquibase:liquibase-core")
    liquibaseRuntime("org.postgresql:postgresql")
    liquibaseRuntime("ch.qos.logback:logback-classic")
    /* picocli needed for liquibase which doesn't include it as a transitive dependency :| */
    liquibaseRuntime("info.picocli:picocli:4.6.3")

    /* Tests */
    testImplementation("org.testcontainers:postgresql:1.17.5")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("org.awaitility:awaitility:4.2.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = javaVersion.toString()
    }
}

tasks.withType<Test> {
    useJUnitPlatform()

    testLogging {
        exceptionFormat = TestExceptionFormat.FULL
        showStandardStreams = false
        events(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
    }
}

tasks.withType<Jar> {
    // don't add version number to generated JAR file to make its name immutable
    archiveVersion.set("")
}

tasks.bootRun {
    systemProperty("spring.profiles.active", "local")
}


tasks.compileKotlin {
    // make sure that we always compile sources of all non-standard source sets
    val standardSourceSetNames = listOf(project.sourceSets.main, project.sourceSets.test).map { it.name }
    val nonStandardSourceSetNames = project.sourceSets.names - standardSourceSetNames
    val nonStandardCompileTasks = nonStandardSourceSetNames.map { "compile${it.capitalize()}Kotlin" }
    finalizedBy(nonStandardCompileTasks)
}

val dbUrl = "jdbc:postgresql://localhost/postgres"
val dbUsername = "postgres"
val dbPassword = "secret"

liquibase {
    activities.register("main") {
        this.arguments = mapOf(
            "logLevel" to "info",
            "changeLogFile" to "src/main/resources/db.changelog/db.changelog-master.xml",
            "url" to dbUrl,
            "username" to dbUsername,
            "password" to dbPassword
        )
    }
    runList = "main"
}

jooq {
    version.set(jooqVersion)
    edition.set(nu.studer.gradle.jooq.JooqEdition.OSS)

    configurations.create("main") {
        generateSchemaSourceOnCompilation.set(false)
        jooqConfiguration.apply {
            logging = org.jooq.meta.jaxb.Logging.WARN
            jdbc.apply {
                driver = "org.postgresql.Driver"
                url = dbUrl
                user = dbUsername
                password = dbPassword
            }
            generator.apply {
                name = "org.jooq.codegen.KotlinGenerator"
                database.apply {
                    name = "org.jooq.meta.postgres.PostgresDatabase"
                    inputSchema = "public"
                    // We do not want to generate classes for Liquibase tables
                    excludes = "databasechangelog|databasechangeloglock"
                }
                generate.apply {
                    isRecords = false
                }
                target.apply {
                    packageName = "com.example.outboxexporter.generated"
                    directory = "./src/generated/kotlin"
                }
                strategy.name = "org.jooq.codegen.DefaultGeneratorStrategy"
            }
        }
    }
}

val postgresImage = "postgres:12.4-alpine"

val startJooqDb by tasks.registering {
    description = "Starts a Postgres instance in docker for the JOOQ generator"
    group = "jooq"

    doFirst {
        // run fresh Postgres for jooq generator
        exec {
            runPostgres(containerName = "jooq-postgres")
        }
        // give it some time to start accepting connections
        Thread.sleep(2_000)
    }
}

val stopJooqDb by tasks.registering {
    description = "Tries to stop the Postgres docker instance for the JOOQ generator"
    group = "jooq"

    doLast {
        exec {
            commandLine("docker", "stop", "jooq-postgres")
        }
    }
}

tasks.update {
    dependsOn(startJooqDb)
}

tasks.named("generateJooq") {
    dependsOn(tasks.update)
    finalizedBy(stopJooqDb)
}

fun ExecSpec.runPostgres(port: Int = 5432, containerName: String) {
    commandLine("docker", "run", "--rm", "-d", "-e", "POSTGRES_PASSWORD=$dbPassword", "-p", "$port:5432", "--name", containerName, postgresImage)
}
