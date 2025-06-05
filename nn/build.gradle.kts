plugins {
    id("application")
    id("buildlogic.java-conventions")
}

dependencies {
    implementation("org.deeplearning4j:deeplearning4j-core:1.0.0-M2.1") {
        exclude(group = "org.bytedeco", module = "opencv-platform")
        exclude(group = "org.bytedeco", module = "ffmpeg-platform")
        exclude(group = "com.twelvemonkeys.imageio", module = "imageio-core")
        exclude(group = "com.twelvemonkeys.imageio", module = "imageio-jpeg")
        exclude(group = "com.twelvemonkeys.imageio", module = "imageio-pnm")
        exclude(group = "com.twelvemonkeys.imageio", module = "imageio-tiff")
        exclude(group = "com.twelvemonkeys.imageio", module = "imageio-bmp")
        exclude(group = "com.twelvemonkeys.imageio", module = "imageio-psd")
        exclude(group = "org.deeplearning4j", module = "deeplearning4j-ui-components")
        exclude(group = "org.bytedeco", module = "javacv")
    }
    implementation("org.nd4j:nd4j-native-platform:1.0.0-M2.1")
    implementation("org.nd4j:nd4j-cuda-11.6-platform:1.0.0-M2.1")
    implementation(project(":logic"))
}

application {
    mainClass = "de.hexgame.nn.Main"
}
