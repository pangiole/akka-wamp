
name := "akka-wamp"

description := "WAMP - Web Application Messaging Protocol implementation written in Scala and based on Akka HTTP and Akka Streams"

organization := "com.github.angiolep"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.11.7"


libraryDependencies ++= Seq (
  "com.typesafe.akka" %% "akka-http-experimental" % "2.4.2",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.7.2",
  
  "com.typesafe.akka" % "akka-http-testkit_2.11" % "2.4.2" % "test",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test"
)

coverageEnabled := true

publishMavenStyle := true

isSnapshot := true

pomExtra :=
  <inceptionYear>2016</inceptionYear>
  <scm>
    <url>git://github.com/akka-wamp/akka-wamp.git</url>
    <connection>scm:git:git@github.com:akka-wamp/akka-wamp.git</connection>
  </scm>
  <developers>
    <developer>
      <name>Paolo Angioletti</name>
      <name>paolo.angioletti@gmail.com</name>
    </developer>
  </developers>
  <licenses>
    <license>
      <name>Apache 2</name>
      <url>https://www.apache.org/licenses/LICENSE-2.0.txt</url>
      <distribution>repo</distribution>
    </license>
  </licenses>


publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")



//publishTo := Some(Resolver.file("file",  new File(Path.userHome.abqsolutePath+"/.m2/repository")))