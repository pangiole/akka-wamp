package akka.wamp.router

import akka.actor._


/**
  * The factory of an embedded router actor and its listeners
  */
object EmbeddedRouter {
  /**
    * Creates an embedded ``router`` actor and spins as many listeners
    * as many configured endpoints for it
    *
    * @param factory is the Akka actor factory
    * @return the router actor reference
    */
  def createAndBind(factory: ActorRefFactory): ActorRef = {
    val router = factory.actorOf(Router.props(), "router")
    factory.actorOf(Props(new Binder(router)))
  }
}