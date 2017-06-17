package akka.wamp.router

import com.typesafe.config.{ConfigFactory => cf}
import akka.actor.ActorSystem
import java.nio.file.Paths

object StandaloneRouterApp extends  App {

  val jvmProps = cf.systemProperties()

  val external =
    if (args != null && args.size > 0)
      cf.parseFile(Paths.get(args(0)).normalize.toFile)
    else
      cf.empty()

  val default = cf.load()

  val config = ( jvmProps /: (external :: default :: Nil))( _ withFallback _)

  val actorSystem = ActorSystem("wamp", config.resolve())
  EmbeddedRouter.createAndBind(actorSystem)
}
