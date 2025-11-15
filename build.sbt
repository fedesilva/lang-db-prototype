import sbt.`*`

lazy val commonSettings =
  Seq(
    organization := "lang-db-prototype",
    version      := "0.0.1-SNAPSHOT",
    scalaVersion := "3.6.4",
    scalacOptions ++= ScalacConfig.opts,
    // because for tests, yolo.
    Test / scalacOptions --= Seq("-Ywarn-unused:imports", "-Xfatal-warnings"),
    Test / scalacOptions --= Seq("-Ywarn-dead-code", "-Ywarn-unused:locals", "-Xfatal-warnings"),
    resolvers ++= Dependencies.resolvers,
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,
    // Set fork to true as recommended by cats-effect for all projects
    Compile / run / fork := true
  )

// Main project
lazy val `lang-db-prototype`: Project =
  project
    .in(file("."))
    .settings(
      commonSettings,
      name := "lang-db-prototype",
      libraryDependencies ++= Dependencies.allDependencies,
      Compile / mainClass := Some("langdb.Main")
    )
