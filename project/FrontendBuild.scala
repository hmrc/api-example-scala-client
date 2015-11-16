import sbt._
import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
object FrontendBuild extends Build with MicroService {

  val appName = "api-example-scala-client"

  override lazy val plugins: Seq[Plugins] = Seq(
    SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin
  )

  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

private object AppDependencies {
  import play.PlayImport._

  private val hmrcPlayJsonLoggerVersion = "2.1.1"
  private val scalaTestVersion = "2.2.5"
  private val scalaTestPlusVersion = "1.2.0"
  private val pegdownVersion = "1.6.0"

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "play-json-logger" % hmrcPlayJsonLoggerVersion
  )

  val test = Seq(
    ws,
    "org.scalatest" %% "scalatest" % scalaTestVersion % "test,it",
    "org.scalatestplus" %% "play" % scalaTestPlusVersion %  "test,it",
    "org.pegdown" % "pegdown" % pegdownVersion % "test,it"
  )

  def apply() = compile ++ test
}


