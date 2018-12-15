name := "blockchain"

version := "0.1"

scalaVersion := "2.12.6"

libraryDependencies ++= Seq(
  "org.apache.httpcomponents" % "httpclient" % "4.5.6",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  "org.mockito" % "mockito-all" % "1.9.5" % "test"
)