inThisBuild(
  List(
    version ~= { dynVer =>
      if (sys.env.contains("TRAVIS_TAG")) dynVer
      else dynVer + "-SNAPSHOT"
    },
    scalaVersion := V.scala212,
    crossScalaVersions := List(V.scala211, V.scala212),
    scalacOptions ++= List(
      "-Yrangepos",
      "-deprecation",
      "-Ywarn-unused-import"
    ),
    libraryDependencies += "io.monix" %% "minitest" % "2.1.1" % "test",
    testFrameworks += new TestFramework("minitest.runner.Framework"),
    // faster publishLocal:
    publishArtifact in packageDoc := sys.env.contains("CI"),
    publishArtifact in packageSrc := sys.env.contains("CI"),
    addCompilerPlugin(
      "org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full
    )
  )
)

name := "lsp4sRoot"

lazy val V = new {
  val scala211 = "2.11.12"
  val scala212 = "2.12.4"
  val monix = "2.3.0"
  val upickle = "0.6.5"
  val scribe = "2.3.3"
  val circe = "0.9.0"
}

noPublish

lazy val macros = project.settings(
  libraryDependencies ++= List(
    "org.scala-lang" % "scala-reflect" % scalaVersion.value % Provided,
    "com.lihaoyi" %% "upickle" % V.upickle
  )
)

lazy val jsonrpc = project
  .settings(
    libraryDependencies ++= List(
      "io.circe" %% "circe-core" % V.circe,
      "io.circe" %% "circe-generic" % V.circe,
      "io.circe" %% "circe-generic-extras" % V.circe,
      "io.circe" %% "circe-parser" % V.circe,
      "com.outr" %% "scribe" % V.scribe,
      "io.monix" %% "monix" % V.monix
    )
  )
  .dependsOn(macros)

lazy val lsp4s = project.dependsOn(jsonrpc)

// For some reason, it doesn't work if this is defined in globalSettings in PublishPlugin.
inScope(Global)(
  Seq(
    PgpKeys.pgpPassphrase := sys.env.get("PGP_PASSPHRASE").map(_.toCharArray())
  )
)
