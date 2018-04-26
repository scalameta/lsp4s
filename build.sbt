inThisBuild(
  List(
    version ~= { dynVer =>
      if (sys.env.contains("TRAVIS_TAG")) dynVer
      else dynVer + "-SNAPSHOT"
    },
    scalaVersion := V.scala212,
    scalacOptions ++= List(
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

lazy val V = new {
  val scala211 = "2.11.11"
  val scala212 = "2.12.4"
  val enumeratum = "1.5.12"
  val circe = "0.9.0"
  val cats = "1.0.1"
  val monix = "2.3.0"
}

lazy val noPublish = List(
  publishTo := None,
  publishArtifact := false,
  skip in publish := true
)

lazy val jsonrpc = project
  .settings(
    crossScalaVersions := List(V.scala211, V.scala212),
    // NOTE: there are plans to drop most of these dependencies
    // https://github.com/scalameta/metals/issues/285
    libraryDependencies ++= List(
      "com.beachape" %% "enumeratum" % V.enumeratum,
      "com.beachape" %% "enumeratum-circe" % "1.5.15",
      "com.lihaoyi" %% "pprint" % "0.5.3",
      "io.circe" %% "circe-core" % V.circe,
      "io.circe" %% "circe-generic" % V.circe,
      "io.circe" %% "circe-generic-extras" % V.circe,
      "io.circe" %% "circe-parser" % V.circe,
      "io.monix" %% "monix" % V.monix,
      "org.typelevel" %% "cats-core" % V.cats
    )
  )

lazy val lsp4s = project
  .settings(
    crossScalaVersions := List(V.scala211, V.scala212)
  )
  .dependsOn(jsonrpc)

// For some reason, it doesn't work if this is defined in globalSettings in PublishPlugin.
inScope(Global)(
  Seq(
    PgpKeys.pgpPassphrase := sys.env.get("PGP_PASSPHRASE").map(_.toCharArray())
  )
)
