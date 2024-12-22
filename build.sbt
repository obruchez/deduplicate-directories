ThisBuild / scalaVersion     := "2.13.12"
ThisBuild / version          := "0.1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .settings(
    name := "Deduplicate Directories"
  )

assembly / mainClass := Some("org.bruchez.olivier.deduplicatedirectories.DeduplicateDirectories")

assembly / assemblyJarName := "deduplicate-directories.jar"
