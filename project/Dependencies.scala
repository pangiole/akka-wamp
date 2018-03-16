import sbt._

object Dependencies {
  val akkaHttpVersion = "10.1.0"
  val akkaVersion = "2.5.11"
  val scalatestVersion = "3.0.1"

  val akkaHttp = "com.typesafe.akka" %% "akka-http" % akkaHttpVersion
  val akkaStream = "com.typesafe.akka" %% "akka-stream" % akkaVersion
  val akkaSlf4j = "com.typesafe.akka" %% "akka-slf4j" % akkaVersion
  val logback = "ch.qos.logback" % "logback-classic" % "1.2.3"
  val jackson = "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.8.4"
  val java8compat = "org.scala-lang.modules" %% "scala-java8-compat" % "0.8.0"


  // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  //  T E S T ing libraries
  // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

  val akkaHttpTestkit = "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion
  //val akkaStreamTestkit = "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion
  val scalatest = "org.scalatest" %% "scalatest" % scalatestVersion
  val scalamock = "org.scalamock" %% "scalamock-scalatest-support" % "3.4.1"
  val mockito = "org.mockito" % "mockito-core" % "2.5.7"
  val pegdown = "org.pegdown" % "pegdown" % "1.6.0"
  val junit = "junit" % "junit" % "4.12"

  val testingLibraries = Seq(
    akkaHttpTestkit,
    //akkaStreamTestkit,
    scalatest,
    scalamock,
    mockito,
    pegdown,
    junit
  )
  .map(_ % Test)
}
