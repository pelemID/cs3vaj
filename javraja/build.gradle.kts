
//import org.jetbrains.kotlin.konan.properties.Properties
// use an integer for version numbers
version = 1

android {
    namespace = "javraja"
//    defaultConfig {
//        val properties = Properties()
//        properties.load(project.rootProject.file("local.properties").inputStream())
//        android.buildFeatures.buildConfig=true
//        buildConfigField("String", "KissKh", "\"${properties.getProperty("KissKh")}\"")
//        buildConfigField("String", "KisskhSub", "\"${properties.getProperty("KisskhSub")}\"")
//    }
}


cloudstream {
    language = "id"
    // All of these properties are optional, you can safely remove them

     description = "Jav Raja"
     authors = listOf("Phisher98")

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
     * */
    status = 1 // will be 3 if unspecified
    tvTypes = listOf("NSFW")

    iconUrl = "https://www.google.com/s2/favicons?domain=kisskh.co&sz=128"

//    isCrossPlatform = true
}