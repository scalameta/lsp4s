inThisBuild(
  List(
    organization := "org.scalameta",
    homepage := Some(url("https://github.com/scalameta/lsp4s")),
    publishMavenStyle := true,
    licenses := Seq(
      "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
    ),
    developers := List(
      Developer(
        "laughedelic",
        "Alexey Alekhin",
        "laughedelic@gmail.com",
        url("https://github.com/laughedelic")
      ),
      Developer(
        "gabro",
        "Gabriele Petronella",
        "gabriele@buildo.io",
        url("https://github.com/gabro")
      ),
      Developer(
        "jvican",
        "Jorge Vicente Cantero",
        "jorgevc@fastmail.es",
        url("https://jvican.github.io/")
      ),
      Developer(
        "olafurpg",
        "Ólafur Páll Geirsson",
        "olafurpg@gmail.com",
        url("https://geirsson.com")
      )
    ),
    scalaVersion := V.scala212,
    crossScalaVersions := List(V.scala211, V.scala212),
    scalacOptions ++= List(
      "-Yrangepos",
      "-Yrangepos",
      "-deprecation",
      // -Xlint is unusable because of
      // https://github.com/scala/bug/issues/10448
      "-Ywarn-unused-import"
    ),
    organization := "org.scalameta",
    licenses := Seq(
      "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
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

skip in publish := true

lazy val V = new {
  val scala211 = "2.11.12"
  val scala212 = "2.12.6"
  val enumeratumCirce = "1.5.18"
  val circe = "0.10.0"
  val circeDerivation = "0.10.0-M1"
  val cats = "1.4.0"
  val monix = "3.0.0-RC1"
}

lazy val jsonrpc = project
  .settings(
    // NOTE: there are plans to drop most of these dependencies
    // https://github.com/scalameta/metals/issues/285
    libraryDependencies ++= List(
      "com.outr" %% "scribe" % "2.6.0",
      "io.circe" %% "circe-core" % V.circe,
      "io.circe" %% "circe-derivation-annotations" % V.circeDerivation,
      "io.circe" %% "circe-parser" % V.circe,
      "io.monix" %% "monix" % V.monix,
      "org.typelevel" %% "cats-core" % V.cats
    )
  )

lazy val lsp4s = project
  .settings(
    libraryDependencies ++= List(
      "com.beachape" %% "enumeratum-circe" % V.enumeratumCirce
    )
  )
  .dependsOn(jsonrpc)
