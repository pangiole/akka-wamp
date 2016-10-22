
val commonSettings = Seq(
  scalaVersion := "2.11.8",
  organization := "com.github.angiolep",
  version := "0.11.0",
  description := "WAMP - Web Application Messaging Protocol implementation written in Scala with Akka HTTP"
)


val akka_wamp_impl = (project in file("./impl"))
  .settings(commonSettings)

val examples_actor = (project in file("./examples/actor"))
  .settings(commonSettings)
  .dependsOn(akka_wamp_impl)

val examples_hello = (project in file("./examples/hello"))
  .settings(commonSettings)
  .dependsOn(akka_wamp_impl)

val examples_router = (project in file("./examples/router"))
  .settings(commonSettings)
  .dependsOn(akka_wamp_impl)

val examples = project
  .settings(commonSettings)
  .aggregate(
    examples_actor,
    examples_hello
  )

val akka_wamp = (project in file("."))
  .settings(commonSettings)
  .aggregate(akka_wamp_impl, examples)
