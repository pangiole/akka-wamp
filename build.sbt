lazy val root = (project in file(".")).
  settings(
    name := "akka-wamp",
    version := "0.1.0",
    scalaVersion := "2.11.7"
  )

libraryDependencies ++= Seq (
  "com.typesafe.akka" % "akka-http-experimental_2.11" % "2.4.2"
)