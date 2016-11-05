package akka.wamp

import akka.actor.{Actor, ActorLogging, Props, Status}
import akka.wamp.messages._

/**
  * It's the manager actor of the [[Wamp]] I/O Extension which is able
  * to process both [[Bind]] and [[Connect]] commands.
  * 
  * It spawns and supervises the [[router.ConnectionListener]] and 
  * [[client.ConnectionHandler]] actors
  */
private[wamp] class Manager extends Actor with ActorLogging {

  override def receive: Receive = {
    case cmd: Bind => {
      val listener = context.actorOf(router.ConnectionListener.props())
      listener.forward(cmd)
    }

    case cmd: Connect =>
      val connector = sender()
      val handler = context.actorOf(client.ConnectionHandler.props(connector))
      handler.forward(cmd)
  }
}


private[wamp] object Manager {
  def props() = Props(new Manager())
}
