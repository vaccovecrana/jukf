plugins { id("io.vacco.oss.gitflow") version "0.9.8" }

group = "io.vacco.jukf"
version = "0.1.0"

configure<io.vacco.oss.gitflow.GsPluginProfileExtension> {
  addJ8Spec()
  addClasspathHell()
  sharedLibrary(true, false)
}

val api by configurations

dependencies {
  api("org.la4j:la4j:0.6.0")
  testImplementation("io.vacco.sabnock:sabnock:0.1.0")
  testImplementation("com.google.code.gson:gson:2.9.0")
}
