import sbt._
import Keys._

object FlatDirectories extends Build {

  lazy val flatDirectoriesSettings: Seq[Setting[_]] = {
    val srcDirectory = Def.setting[File](baseDirectory.value / "src")
    val scalaTestSourcesPattern = "*Test.scala"
    val sourcesFilePattern: FileFilter = "*.scala" || "*.java"
    Seq(
      scalaSource       in Compile <<= srcDirectory,
      scalaSource       in Test <<= srcDirectory,
      javaSource        in Compile <<= srcDirectory,
      javaSource        in Test <<= srcDirectory,
      excludeFilter     in (Compile, unmanagedSources) ~= { _ || scalaTestSourcesPattern },
      excludeFilter     in (Test, unmanagedSources) ~= { _  -- scalaTestSourcesPattern },
      includeFilter     in (Test, unmanagedSources) := scalaTestSourcesPattern,
      resourceDirectory in Compile<<= srcDirectory,
      resourceDirectory in Test<<= srcDirectory,
      excludeFilter     in (Compile, unmanagedResources) ~= { _ || sourcesFilePattern },
      excludeFilter     in (Test, unmanagedResources)  ~= { _ || sourcesFilePattern }
    )
  }

}