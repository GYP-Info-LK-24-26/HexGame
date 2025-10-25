plugins {
    id("application")
    id("buildlogic.java-conventions")
}

val tfVersion = "0.5.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.tensorflow:tensorflow-core-platform-gpu:$tfVersion")
    implementation("org.tensorflow:tensorflow-framework:${tfVersion}")
    implementation("org.apache.commons:commons-statistics-distribution:1.2")
    implementation(project(":logic"))
}

application {
    mainClass = "de.hexgame.nn.Main"
}
