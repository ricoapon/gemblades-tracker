plugins {
    id("java")
}

group = "nl.ricoapon"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // SQLite JDBC driver.
    implementation("org.xerial:sqlite-jdbc:3.53.2.1")

    // JDBI: lightweight layer over JDBC with declarative SqlObject DAOs.
    implementation("org.jdbi:jdbi3-core:3.54.0")
    implementation("org.jdbi:jdbi3-sqlobject:3.54.0")

    // Flyway for versioned schema migrations. flyway-core bundles the SQLite engine,
    // so no separate database module is needed.
    implementation("org.flywaydb:flyway-core:13.2.0")

    // Simple logging.
    runtimeOnly("org.slf4j:slf4j-simple:2.0.18")

    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}