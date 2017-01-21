package akka.wamp.router

import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem

object StandaloneRouterApp extends  App {

  val file = System.getProperty("config.file")
  val config =
    if (file == null) ConfigFactory.load()
    else ConfigFactory.load(file)

  val systemProperties = ConfigFactory.systemProperties()
  val actorSystem = ActorSystem("wamp", systemProperties.withFallback(config))
  EmbeddedRouter.createAndBind(actorSystem)
}
