import Dependencies._

organization := "com.github.angiolep"

name := "akka-wamp"

version := "0.5.0"

scalaVersion := "2.11.8"


description := "WAMP - Web Application Messaging Protocol implementation written in Scala with Akka"

mainClass in Compile := Some("akka.wamp.WebSocketRouter")

libraryDependencies ++= Seq (
  akkaHttp, 
  akkaSlf4j, 
  logback, 
  jackson,
  scalactic
) ++ Seq(
  akkaHttpTestkit, 
  scalatest,
  scalamock,
  pegdown
).map(_ % Test)

testOptions in Test += Tests.Setup { () =>
  System.setProperty("akka.loglevel", "error")
  System.setProperty("akka.stdout-loglevel", "error")
}

parallelExecution in Test := true

publishMavenStyle := true

isSnapshot := false

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

licenses := Seq("Apache 2" -> url("https://www.apache.org/licenses/LICENSE-2.0.txt"))

homepage := Some(url("http://angiolep.github.io/akka-wamp"))

pomExtra :=
  <scm>
    <url>git://github.com/angiolep/akka-wamp.git</url>
    <connection>scm:git:git@github.com:angiolep/akka-wamp.git</connection>
  </scm>
    <developers>
      <developer>
        <name>Paolo Angioletti</name>
        <email>paolo.angioletti@gmail.com</email>
        <url>http://angiolep.github.io</url>
      </developer>
    </developers>

apiURL := Some(url("http://angiolep.github.io/projects/akka-wamp/doc/index.html"))

credentials += Credentials(Path.userHome / ".ivy2" / "sonatype")

enablePlugins(JavaAppPackaging)
