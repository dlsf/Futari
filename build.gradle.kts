plugins {
    id("java")
    id("application")
}

group = "moe.das"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.xerial:sqlite-jdbc:3.46.1.0")
    implementation("ch.qos.logback:logback-classic:1.5.8")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.17.2")

    implementation("net.dv8tion:JDA:5.1.1") {
        exclude("opus-java")
    }
}

application {
    mainClass.set("moe.das.futari.Main")
}
