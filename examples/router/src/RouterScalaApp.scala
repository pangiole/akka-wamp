import akka.actor._
import akka.wamp.router._

object RouterScalaApp extends App {

  // Either the actor system or an actor context
  val actorFactory = ActorSystem()

  // Just create and bind the router actor as per its configuration
  EmbeddedRouter.createAndBind(actorFactory)

  // ...

}
