plugins {
    id("application")
    id("buildlogic.java-conventions")
}

dependencies {
    implementation("org.deeplearning4j:deeplearning4j-core:1.0.0-M2.1")
    implementation("org.nd4j:nd4j-native-platform:1.0.0-M2.1")
    implementation(project(":logic"))
}

application {
    mainClass = "de.hexgame.nn.Main"
}
