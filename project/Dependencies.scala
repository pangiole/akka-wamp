import sbt._

object Dependencies {
  val akkaVersion = "2.4.10"
  val scalatestVersion = "2.2.6"
  
  val akkaHttp = "com.typesafe.akka" %% "akka-http-experimental" % akkaVersion
  val akkaHttpTestkit = "com.typesafe.akka" %% "akka-http-testkit" % akkaVersion
  val akkaSlf4j = "com.typesafe.akka" %% "akka-slf4j" % akkaVersion
  val logback = "ch.qos.logback" % "logback-classic" % "1.1.3"
  val jackson = "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.7.2"
  val scalatest = "org.scalatest" %% "scalatest" % scalatestVersion
  val scalamock = "org.scalamock" %% "scalamock-scalatest-support" % "3.2.2"
  val pegdown = "org.pegdown" % "pegdown" % "1.6.0"
}