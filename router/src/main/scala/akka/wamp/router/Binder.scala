package akka.wamp.router

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.io.IO
import akka.wamp.messages.{Bind, Bound, CommandFailed}

import scala.collection.JavaConverters._



private class Binder(router: ActorRef) extends Actor with ActorLogging {
  implicit val actorSystem = context.system
  val config = actorSystem.settings.config
  val manager = IO(Wamp)

  override def preStart(): Unit = {
    config.getObject("akka.wamp.router.endpoint")
      .unwrapped()
      .asScala
      .foreach { case (endpoint, _) =>
        manager ! Bind(router, endpoint)
      }
  }

  override def receive: Receive = {
    case CommandFailed(cmd, ex) =>
      log.warning(s"$cmd failed because of $ex")

    case Bound(_, uri) =>
      log.debug(s"Bound to $uri")
  }
}
