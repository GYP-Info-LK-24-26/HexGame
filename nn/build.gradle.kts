plugins {
    id("buildlogic.java-conventions")
}

val tfVersion = "1.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.tensorflow:tensorflow-core-platform:${tfVersion}")
    implementation("org.tensorflow:tensorflow-framework:${tfVersion}")
    implementation(project(":logic"))
}
