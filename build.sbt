import sbt.Tests.{Group, SubProcess}
import uk.gov.hmrc.DefaultBuildSettings.{addTestReportOption, defaultSettings, scalaSettings, targetJvm}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

lazy val plugins: Seq[Plugins] = Seq(
  SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory
)
lazy val playSettings: Seq[Setting[_]] = Seq.empty

lazy val microservice = (project in file("."))
  .enablePlugins(Seq(play.sbt.PlayScala) ++ plugins: _*)
  .settings(
    name := appName
  )
  .settings(playSettings: _*)
  .settings(scalaSettings: _*)
  .settings(publishingSettings: _*)
  .settings(defaultSettings(): _*)
  .settings(
    targetJvm := "jvm-1.8",
    libraryDependencies ++= appDependencies,
    parallelExecution in Test := false,
    fork in Test := false,
    retrieveManaged := true,
    routesGenerator := StaticRoutesGenerator,
    majorVersion := 0
  )
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(Defaults.testSettings): _*)
  .settings(
    Keys.fork in IntegrationTest := false,
    unmanagedSourceDirectories in IntegrationTest <<= (baseDirectory in IntegrationTest) (base => Seq(base / "it")),
    addTestReportOption(IntegrationTest, "int-test-reports"),
    testGrouping in IntegrationTest := (definedTests in IntegrationTest).value.map {
      test => Group(test.name, Seq(test), SubProcess(ForkOptions(runJVMOptions = Seq("-Dtest.name=" + test.name))))
    },
    parallelExecution in IntegrationTest := false)
  .settings(
    resolvers := Seq(
      Resolver.bintrayRepo("hmrc", "releases"),
      Resolver.typesafeRepo("releases"),
      Resolver.jcenterRepo
    )
  )
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
  .settings(scalaVersion := "2.11.11")

lazy val appName = "api-example-scala-client"
lazy val appDependencies: Seq[ModuleID] = allDeps

val compile = Seq(
  "uk.gov.hmrc" %% "logback-json-logger" % "3.1.0",
  "uk.gov.hmrc" %% "bootstrap-play-25" % "4.8.0"
)

val test = Seq(
  ws,
  "org.scalatest" %% "scalatest" % "2.2.6" % "test,it",
  "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0" % "test,it",
  "org.mockito" % "mockito-core" % "2.10.0" % "test,it",
  "org.pegdown" % "pegdown" % "1.6.0" % "test,it"
)
lazy val allDeps = compile ++ test
