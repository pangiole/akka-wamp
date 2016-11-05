package akka.wamp.router

import akka.actor._
import akka.io._
import akka.wamp._
import akka.wamp.messages._


class EmbeddedRouter(factory: ActorRefFactory)(implicit system: ActorSystem) extends Actor with ActorLogging {

  override def preStart(): Unit = {
    val router = factory.actorOf(Router.props(), "router")
    val manager = IO(Wamp)
    manager ! Bind(router)
  }

  override def receive: Receive = {
    case signal @ CommandFailed(cmd, ex) =>
      log.warning(s"$cmd failed because of $ex")

    case signal @ Bound(listener, url) =>
      log.debug(s"Bound to $url")
    // ...
    // listener ! Unbind
  }
}

object EmbeddedRouter {
  def createAndBind(factory: ActorRefFactory)(implicit system: ActorSystem): ActorRef = {
    factory.actorOf(Props(new EmbeddedRouter(factory)))
  }
  def createAndBind(implicit system: ActorSystem): ActorRef = {
    system.actorOf(Props(new EmbeddedRouter(system)))
  }
}