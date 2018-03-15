import sbt._
import Keys._

object Common {
  val coreSettings: Seq[Setting[_]] = Seq(
    crossScalaVersions := Seq(/*"2.11.11", */"2.12.2"),
    scalaVersion := "2.12.2",
    scalacOptions := Seq("-unchecked", "-deprecation"),
    organization := "com.github.angiolep",
    version := "0.15.1",
    description := "WAMP - Web Application Messaging Protocol implementation written in Scala/Java8 with Akka HTTP"
  )

  val exampleSettings: Seq[Setting[_]] = coreSettings ++ Seq(
//    scalacOptions += "-Ymacro-debug-lite",
    publishArtifact := false
//    libraryDependencies ++= Seq(
//      "org.scala-lang" % "scala-compiler" % scalaVersion.value
//    )
  )

}
