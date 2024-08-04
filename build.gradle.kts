plugins {
    id("java")
    id("application")
}

group = "net.dasunterstrich"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.xerial:sqlite-jdbc:3.46.0.1")
    implementation("ch.qos.logback:logback-classic:1.5.6")

    implementation("net.dv8tion:JDA:5.0.2") {
        exclude("opus-java")
    }
}

application {
    mainClass.set("net.dasunterstrich.futari.Main")
}
