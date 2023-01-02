plugins {
    id("java")
}

group = "net.dasunterstrich"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("net.dv8tion:JDA:5.0.0-beta.2") {
        exclude("opus-java")
    }
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}