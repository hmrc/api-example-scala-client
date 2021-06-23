import play.core.PlayVersion
import sbt._

object AppDependencies {
  def apply(): Seq[ModuleID] = dependencies ++ testDependencies
  
  private lazy val dependencies = Seq(
    "uk.gov.hmrc"             %% "bootstrap-play-26"            % "4.0.0"
  )

  private lazy val testDependencies = Seq(
    "org.scalatestplus.play"  %% "scalatestplus-play"           % "3.1.3",
    "org.mockito"             %% "mockito-scala-scalatest"      % "1.7.1",
    "org.pegdown"             %  "pegdown"                      % "1.6.0"
  ).map(d => d % "test, it")
}
