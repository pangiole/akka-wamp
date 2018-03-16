package akka.wamp.router

import javax.net.ssl.SSLContext

import akka.actor.{Actor, ActorLogging, Props}
import akka.wamp.SSLContextHolder
import akka.wamp.messages.Bind

private[wamp] class Manager extends Actor with ActorLogging {
  implicit val actorSystem = context.system

  val config = actorSystem.settings.config

  var sslContext:SSLContext = _

  override def preStart(): Unit = {
    sslContext = SSLContextHolder(this).sslContext
  }


  override def receive: Receive = {
    case cmd @ Bind(router, endpoint) => {
      val binder = sender()
      context.actorOf(ConnectionListener.props(binder, router, endpoint, sslContext))
    }
  }
}


private[wamp] object Manager {
  def props() = Props(new Manager())
}
