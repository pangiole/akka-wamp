
val commonSettings = Seq(
  scalaVersion := "2.11.8",
  organization := "com.github.angiolep",
  version := "0.10.0",
  description := "WAMP - Web Application Messaging Protocol implementation written in Scala with Akka HTTP"
)


val peers = project
  .settings(commonSettings)


val examples = project
  .settings(commonSettings)
  .dependsOn(peers)


val root = (project in file("."))
  .settings(commonSettings)
  .aggregate(peers, examples)
