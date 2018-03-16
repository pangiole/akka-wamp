package akka.wamp.client

import javax.net.ssl.SSLContext

import akka.actor.{Actor, ActorLogging, Props}
import akka.wamp.SSLContextHolder
import akka.wamp.messages.Connect

private[wamp] class Manager extends Actor with ActorLogging {
  implicit val actorSystem = context.system

  val config = actorSystem.settings.config

  var sslContext:SSLContext = _

  override def preStart(): Unit = {
    sslContext = SSLContextHolder(this).sslContext
  }


  override def receive: Receive = {
    case cmd @ Connect(address, format) =>
      val connector = sender()
      context.actorOf(ConnectionHandler.props(connector, address, format, sslContext))
  }
}


private[wamp] object Manager {
  def props() = Props(new Manager())
}
