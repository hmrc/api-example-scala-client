import sbt.Tests.{Group, SubProcess}
import uk.gov.hmrc.DefaultBuildSettings.{addTestReportOption, defaultSettings, scalaSettings}
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
    libraryDependencies ++= AppDependencies(),
    Test / parallelExecution := false,
    Test / fork := false,
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
      (IntegrationTest / definedTests).value
    ),
    IntegrationTest / parallelExecution := false
  )

  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
  .settings(scalaVersion := "2.12.12")

lazy val appName = "api-example-scala-client"

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
