object project extends ProjectSettings {
  def scalaVersion = "2.10.4"
  def version = "1.1.0"
  def name = "io"
  def description = "Rapture IO is a general purpose IO library for Scala, providing much of the functionality of java.io and java.net with an idiomatic Scala API"
  
  def dependencies = Seq(
    "codec" -> "1.1.0",
    "uri" -> "1.1.0",
    "mime" -> "1.1.0"
  )
  
  def thirdPartyDependencies = Nil

  def imports = Seq(
    "rapture.core._",
    "rapture.mime._",
    "rapture.uri._",
    "rapture.io._"
  )
}
