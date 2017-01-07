
val core = project
  .settings(Common.settings)

val docs = project
  .settings(Common.settings)
  .dependsOn(core)

val actors = (project in file("./examples/actors"))
  .settings(Common.examples)
  .dependsOn(core)

val futures = (project in file("./examples/futures"))
  .settings(Common.examples)
  .dependsOn(core)

val router = (project in file("./examples/router"))
  .settings(Common.examples)
  .dependsOn(core)

val streams = (project in file("./examples/streams"))
  .settings(Common.examples)
  .dependsOn(core)


val root = (project in file("."))
  .settings(Common.settings)
  .aggregate(
    docs,
    actors,
    futures,
    router,
    streams,
    core
  )