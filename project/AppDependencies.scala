import play.core.PlayVersion
import sbt._

object AppDependencies {
  def apply(): Seq[ModuleID] = dependencies ++ testDependencies
  
  private lazy val dependencies = Seq(
    "uk.gov.hmrc"             %% "bootstrap-frontend-play-28"             % "7.12.0",
    "uk.gov.hmrc"             %% "play-ui"                                % "9.11.0-play-28",
    "uk.gov.hmrc"             %% "govuk-template"                         % "5.78.0-play-28"
  )

  private lazy val testDependencies = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"                 % "7.12.0",
    "org.mockito"             %% "mockito-scala-scalatest"                % "1.7.1",
  ).map(_ % "test, it")
}
