object project extends ProjectSettings {
  def scalaVersion = "2.11.0-RC4"
  def version = "0.9.0"
  def name = "io"
  def description = "Rapture IO is a general purpose IO library for Scala, providing much of the functionality of java.io and java.net with an idiomatic Scala API"
  
  def dependencies = Seq(
    "core" -> "0.9.0",
    "uri" -> "0.9.1",
    "mime" -> "0.9.0"
  )
  
  def thirdPartyDependencies = Nil

  def imports = Seq(
    "rapture.core._",
    "rapture.mime._",
    "rapture.uri._",
    "rapture.io._"
  )
}
