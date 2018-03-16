
import Dependencies._
import sbt.Keys.{crossScalaVersions, _}
import sbt.Test
import sbt.io._


val commonSettings = Seq(
  Test / testOptions += Tests.Setup { () =>
    System.setProperty("akka.loglevel", "off")
    System.setProperty("akka.stdout-loglevel", "off")
  },
  // TODO parallelExecution in Test := true,

  // read sbt-sonatype settings from sonatype.sbt file
  publishTo := sonatypePublishTo.value
)



val core = (project in file("./core"))
  .settings(
    commonSettings,
    name := "akka-wamp-core",
    libraryDependencies ++= Seq (
      akkaHttp,
      akkaStream,
      akkaSlf4j,
      logback,
      jackson
    ) ++ testingLibraries
  )



val router = (project in file("./router"))
  .enablePlugins(JavaAppPackaging)
  .settings(
    commonSettings,
    name := "akka-wamp-router",
    libraryDependencies ++= testingLibraries,
    Compile / mainClass := Some("akka.wamp.router.StandaloneRouterApp"),

    // sbt-native-packages settings
    Universal / mappings  ++= Seq(
      ((Compile / resourceDirectory).value) / "application.conf" -> "conf/application.conf",
      ((Compile / resourceDirectory).value) / "logback.xml"      -> "conf/logback.xml"
    ),

    Universal / bashScriptExtraDefines += """addApp "${app_home}/../conf/application.conf" """,
  )
  .dependsOn(
    core % "compile->compile"
  )



val client = (project in file("./client"))
  .settings(
    commonSettings,
    name := "akka-wamp-client",
    libraryDependencies ++= testingLibraries,
    // TODO scalacOptions in (ScalaUnidoc, unidoc) += "-Ymacro-expand:none"
  )
  .dependsOn(
    core % "compile->compile",
    router % "test->compile"
  )



val macros = (project in file("./macros"))
  .settings(
    commonSettings,
    name := "akka-wamp-macros",
    libraryDependencies ++= testingLibraries,
    scalacOptions += "-language:experimental.macros"
  )
  .dependsOn(
    client % "compile->compile",
    router % "test->compile"
  )



// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//
//    D O C s
//
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

val userdocs = (project in file("./userdocs"))
  //.enablePlugins(ParadoxPlugin)
  .enablePlugins(AkkaParadoxPlugin)
  .settings(
    // paradoxTheme := Some(builtinParadoxTheme("generic")),
    paradoxNavigationDepth := 3,
    publishArtifact := false
  )
  .dependsOn(client)




// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//
//    E X A M P L E s
//
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

val exampleSettings = Seq(
  publishArtifact := false,
  Compile / scalaSource := baseDirectory.value / "src",
  Compile / javaSource := baseDirectory.value / "src"
  // DEBUG scalacOptions += "-Ymacro-debug-lite",
  // DEBUG libraryDependencies += "org.scala-lang" % "scala-compiler" % scalaVersion.value
)


val `example-router` = (project in file("./examples/router"))
  .settings(
    exampleSettings
  )
  .dependsOn(router)



val `example-actors` = (project in file("./examples/actors"))
  .settings(
    exampleSettings,
  )
  .dependsOn(client)



val `example-futures` = (project in file("./examples/futures"))
  .settings(
    exampleSettings
  )
  .dependsOn(client)



val `example-macros` = (project in file("./examples/macros"))
  .settings(
    exampleSettings
  )
  .dependsOn(macros)



val `example-poloniex` = (project in file("./examples/poloniex"))
  .settings(
    exampleSettings
  )
  .dependsOn(client)



val `example-streams` = (project in file("./examples/streams"))
  .settings(
    exampleSettings
  )
  .dependsOn(client)




// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//
//    R O O T
//
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

val root = (project in file("."))
  .enablePlugins(ScalaUnidocPlugin)
  .settings(
    inThisBuild(List(
      organization       := "com.github.angiolep",
      // name
      version            := "0.15.2",
      description        := "WAMP - Web Application Messaging Protocol implementation written in Scala/Java8 with Akka HTTP",

      crossScalaVersions := Seq(/*"2.11.11", */"2.12.4"),
      scalaVersion       := "2.12.4",
      scalacOptions      := Seq("-unchecked", "-deprecation", "-feature")
    )),
    ScalaUnidoc / unidoc /unidocProjectFilter := inAnyProject -- inProjects(
      userdocs,
      macros,
      `example-actors`,
      `example-futures`,
      `example-poloniex`,
      `example-router`,
      `example-macros`,
      `example-streams`
    )
    // TODO ,scalacOptions in (ScalaUnidoc, unidoc) += "-Ymacro-expand:none"
  )
  .aggregate(
    core,
    router,
    client,
    macros,
    `example-actors`,
    `example-futures`,
    `example-poloniex`,
    `example-router`,
    `example-macros`,
    `example-streams`
  )
