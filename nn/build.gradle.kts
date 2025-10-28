plugins {
    id("application")
    id("buildlogic.java-conventions")
}

val tfVersion = "1.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.tensorflow:tensorflow-core-api:${tfVersion}")
    implementation("org.tensorflow:tensorflow-core-native:${tfVersion}:windows-x86_64")
    implementation("org.tensorflow:tensorflow-framework:${tfVersion}")
    implementation("org.apache.commons:commons-statistics-distribution:1.2")
    implementation(project(":logic"))
}

application {
    mainClass = "de.hexgame.nn.Main"
}
