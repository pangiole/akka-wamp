
name := "akka-wamp"

description := "WAMP - Web Application Messaging Protocol implementation written in Scala with Akka HTTP"

organization := "com.github.angiolep"

version := "0.1.0"

scalaVersion := "2.11.7"

enablePlugins(JavaAppPackaging)

mainClass in Compile := Some("akka.wamp.WebSocketRouter")

libraryDependencies ++= Seq (
  "com.typesafe.akka" %% "akka-http-experimental" % "2.4.4",
  "com.typesafe.akka" %% "akka-slf4j" % "2.4.4",
  "ch.qos.logback" % "logback-classic" % "1.1.3",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.7.2",
  "com.typesafe.akka" % "akka-http-testkit_2.11" % "2.4.2" % "test",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test"
)

testOptions in Test += Tests.Setup( () => System.setProperty("akka.loglevel", "WARN") )

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
    <url>git://github.com/akka-wamp/akka-wamp.git</url>
    <connection>scm:git:git@github.com:akka-wamp/akka-wamp.git</connection>
  </scm>
  <developers>
    <developer>
      <name>Paolo Angioletti</name>
      <email>paolo.angioletti@gmail.com</email>
      <url>http://angiolep.github.io</url>
    </developer>
  </developers>

credentials += Credentials(Path.userHome / ".ivy2" / "sonatype")
