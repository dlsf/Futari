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
    implementation("com.zaxxer:HikariCP:6.2.1")
    implementation("org.xerial:sqlite-jdbc:3.49.1.0")
    implementation("ch.qos.logback:logback-classic:1.5.17")

    implementation("net.dv8tion:JDA:5.3.0") {
        exclude("opus-java")
    }
}

application {
    mainClass.set("moe.das.futari.Main")
}
