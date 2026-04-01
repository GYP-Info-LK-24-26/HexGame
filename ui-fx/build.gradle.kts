plugins {
    id("buildlogic.java-conventions")
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
}

repositories {
    mavenCentral()
}

javafx {
    version = "22"
    modules("javafx.controls")
}

dependencies {
    implementation(project(":logic"))
    implementation(project(":algorithm"))
    implementation(project(":nn"))
    implementation("io.netty:netty-all:5.0.0.Alpha2")
    implementation("com.google.code.gson:gson:2.10.1")
}

application {
    mainClass.set("de.hexgame.uifx.Main")
}
