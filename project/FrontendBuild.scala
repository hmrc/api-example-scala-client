import sbt._
import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning

object FrontendBuild extends Build with MicroService {

  val appName = "api-example-scala-client"

  override lazy val plugins: Seq[Plugins] = Seq(
    SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin
  )

  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

private object AppDependencies {

  import play.sbt.PlayImport._

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "logback-json-logger" % "3.1.0"
  )

  val test = Seq(
    ws,
    "org.scalatest" %% "scalatest" % "2.2.5" % "test,it",
    "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % "test,it",
    "org.mockito" % "mockito-core" % "1.9.0" % "test,it",
    "org.pegdown" % "pegdown" % "1.6.0" % "test,it"
  )

  def apply() = compile ++ test
}
