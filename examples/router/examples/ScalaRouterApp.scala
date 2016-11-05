package examples

import akka.actor.ActorSystem
import akka.wamp.router.EmbeddedRouter

object ScalaRouterApp extends App {
  val factory = ActorSystem()
  EmbeddedRouter.createAndBind(factory)

  // ...
}
