plugins {
  alias(libs.plugins.androidApplication) apply false
  alias(libs.plugins.kotlinAndroid) apply false
  alias(libs.plugins.kotlinKapt) apply false
  alias(libs.plugins.kotlinParcelize) apply false
  alias(libs.plugins.ksp) apply false
  alias(libs.plugins.hilt) apply false
  alias(libs.plugins.compose.compiler) apply false
}

tasks.register("clean", Delete::class) {
  delete(rootProject.layout.buildDirectory)
}
