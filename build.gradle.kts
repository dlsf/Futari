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
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("org.xerial:sqlite-jdbc:3.40.1.0")
    implementation("ch.qos.logback:logback-classic:1.4.7")

    implementation("net.dv8tion:JDA:5.0.0-beta.20") {
        exclude("opus-java")
    }
}

application {
    mainClass.set("net.dasunterstrich.futari.Main")
}
