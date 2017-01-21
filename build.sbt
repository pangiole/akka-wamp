
import Common._

val core = project
  .settings(coreSettings)

val docs = project
  .settings(coreSettings)
  .dependsOn(core)


val actors = (project in file("./examples/actors"))
  .settings(exampleSettings)
  .dependsOn(core)

val futures = (project in file("./examples/futures"))
  .settings(exampleSettings)
  .dependsOn(core)

val poloniex = (project in file("./examples/poloniex"))
  .settings(exampleSettings)
  .dependsOn(core)

val router = (project in file("./examples/router"))
  .settings(exampleSettings)
  .dependsOn(core)

val streams = (project in file("./examples/streams"))
  .settings(exampleSettings)
  .dependsOn(core)



val root = (project in file("."))
  .settings(coreSettings)
  .aggregate(core)

