import java.time.LocalDate
import java.time.format.DateTimeFormatter

val dateVersion = LocalDate.now().format(DateTimeFormatter.ofPattern("yy.MM.dd"))

plugins {
    `java-library`
    `maven-publish`
}

group = "com.github.Latikeshh"
version = dateVersion

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    // Standard JVM library with zero external dependencies
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = "com.github.Latikeshh"
            artifactId = "databurner-lib"
            version = dateVersion
        }
    }
}
