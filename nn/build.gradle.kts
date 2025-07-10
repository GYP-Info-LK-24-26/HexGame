plugins {
    id("application")
    id("buildlogic.java-conventions")
}

val dl4j        = "1.0.0-M2.1"
val javacppCuda = "11.6-8.3-1.5.7"   // JavaCPP 1.5.7 presets that match DL4J M2.1

repositories {
    mavenCentral()
}

dependencies {
    // ND4J CUDA backend ─ Java code + native libs
    implementation("org.nd4j:nd4j-cuda-11.6:$dl4j")
    runtimeOnly("org.nd4j:nd4j-cuda-11.6:$dl4j:windows-x86_64-cudnn")

    // CUDA runtime (brings jnicudart + CUDA DLLs)  **← NEW**
    runtimeOnly("org.bytedeco:cuda-platform-redist:$javacppCuda")

    // DL4J itself
    implementation("org.deeplearning4j:deeplearning4j-core:$dl4j")
    implementation(project(":logic"))
}

application {
    mainClass = "de.hexgame.nn.Main"
}
