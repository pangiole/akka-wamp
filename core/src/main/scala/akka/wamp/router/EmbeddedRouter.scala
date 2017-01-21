package akka.wamp.router

import akka.actor._
import akka.io._
import akka.wamp._
import akka.wamp.messages._

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
    case sig @ CommandFailed(cmd, ex) =>
      log.warning(s"$cmd failed because of $ex")

    case sig @ Bound(_, url) =>
      log.debug(s"Bound to $url")
  }
}


/**
  * The factory of an embedded router actor and its listeners
  */
object EmbeddedRouter {
  /**
    * Creates an embedded router actor and spins as many listeners
    * as many configured endpoints for it
    *
    * @param actorSystem is the Akka Actor System
    * @return the embedded router
    */
  def createAndBind(actorSystem: ActorSystem): ActorRef = {
    val router = actorSystem.actorOf(Router.props(), "router")
    actorSystem.actorOf(Props(new Binder(router)))
  }
}