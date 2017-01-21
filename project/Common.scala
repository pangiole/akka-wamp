import sbt._
import Keys._

object Common {
  val coreSettings: Seq[Setting[_]] = Seq(
    crossScalaVersions := Seq(/* TODO "2.11.8", */"2.12.1"),
    scalaVersion := "2.12.1",
    scalacOptions := Seq("-unchecked", "-deprecation"),
    organization := "com.github.angiolep",
    version := "0.14.0",
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