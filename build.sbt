import sbt.Tests.{Group, SubProcess}
import uk.gov.hmrc.DefaultBuildSettings.{addTestReportOption, defaultSettings, scalaSettings, targetJvm}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

lazy val plugins: Seq[Plugins] = Seq(SbtAutoBuildPlugin, SbtDistributablesPlugin)

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
    majorVersion := 0
  )
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(Defaults.testSettings): _*)
  .settings(
    IntegrationTest / fork := false,
    IntegrationTest / unmanagedSourceDirectories += baseDirectory.value / "it",
    addTestReportOption(IntegrationTest, "int-test-reports"),
    IntegrationTest / testGrouping := oneForkedJvmPerTest(
      (definedTests in IntegrationTest).value
    ),
    IntegrationTest / parallelExecution := false
  )
  .settings(
      resolvers += "hmrc-releases" at "https://artefacts.tax.service.gov.uk/artifactory/hmrc-releases/"
  )
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
  .settings(scalaVersion := "2.12.12")

lazy val appName = "api-example-scala-client"
lazy val appDependencies: Seq[ModuleID] = allDeps

val compile = Seq(
  ws,
  "uk.gov.hmrc"             %% "bootstrap-play-26"            % "4.0.0"
)

val test = Seq(
  "org.scalatestplus.play"  %% "scalatestplus-play"           % "3.1.3"     % "test,it",
  "org.mockito"             %% "mockito-scala-scalatest"      % "1.7.1"     % "test,it",
  "org.pegdown"             %  "pegdown"                      % "1.6.0"     % "test,it"
)

lazy val allDeps = compile ++ test

def oneForkedJvmPerTest(tests: Seq[TestDefinition]): Seq[Group] =
  tests map { test =>
    Group(
      test.name,
      Seq(test),
      SubProcess(
        ForkOptions().withRunJVMOptions(Vector(s"-Dtest.name=${test.name}"))
      )
    )
  }
