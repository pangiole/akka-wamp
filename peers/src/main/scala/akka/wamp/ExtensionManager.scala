package akka.wamp

import akka.actor.{Actor, ActorLogging, Props}
import akka.wamp.messages._

/**
  * It's the manager actor of the [[Wamp]] I/O Extension which is able
  * to process both [[Bind]] and [[Connect]] commands.
  * 
  * It spawns and supervises the [[router.TransportListener]] and 
  * [[client.TransportHandler]] actors
  */
private[wamp] class ExtensionManager extends Actor with ActorLogging {

  override def receive: Receive = {
    case cmd: Bind => {
      val listener = context.actorOf(router.TransportListener.props())
      listener.forward(cmd)
    }
      
    case cmd: Connect => 
      val handler = context.actorOf(client.TransportHandler.props())
      handler.forward(cmd)
  }
}


private[wamp] object ExtensionManager {
  def props() = Props(new ExtensionManager())
}
