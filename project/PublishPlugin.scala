import com.typesafe.sbt.SbtPgp.autoImport.PgpKeys
import sbt.Def
import sbt._
import sbt.Keys._
import sbt.plugins.JvmPlugin
import sys.process._

object PublishPlugin extends AutoPlugin {

  override def trigger = allRequirements
  override def requires = JvmPlugin

  private def isTravisSecure =
    sys.env.get("TRAVIS_SECURE_ENV_VARS").contains("true")

  override def globalSettings: Seq[Def.Setting[_]] = List(
    commands += Command.command("ci-release") { s =>
      if (!isTravisSecure) {
        println(s"Skipping publish, branch=${sys.env.get("TRAVIS_BRANCH")}")
        s
      } else {
        println("Setting up gpg")
        "git log HEAD~20..HEAD".!
        (s"echo ${sys.env("PGP_SECRET")}" #| "base64 --decode" #| "gpg --import").!
        println("Publishing release")
        "+publishSigned" ::
          "sonatypeReleaseAll" ::
          s
      }
    },
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
    )
  )

  override def projectSettings: Seq[Def.Setting[_]] = List(
    publishTo := Some {
      if (isSnapshot.value) Opts.resolver.sonatypeSnapshots
      else Opts.resolver.sonatypeStaging
    }
  )

}
