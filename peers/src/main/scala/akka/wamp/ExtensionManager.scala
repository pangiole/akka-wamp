package akka.wamp

import akka.actor.{Actor, ActorLogging, Props}

/**
  * It's the manager actor of the [[Wamp]] I/O Extension which is able
  * to process both [[Wamp.Bind]] and [[Wamp.Connect]] commands.
  * 
  * It spawns and supervises the [[router.TransportListener]] and [[client.Manager]]
  * actors
  */
private[wamp] class ExtensionManager extends Actor with ActorLogging {

  override def receive: Receive = {
    case cmd: Wamp.Bind => {
      val listener = context.actorOf(router.TransportListener.props())
      listener.forward(cmd)
    }
      
    case cmd: Wamp.Connect => 
      val manager = context.actorOf(client.Manager.props())
      manager.forward(cmd)
  }
}


private[wamp] object ExtensionManager {
  def props() = Props(new ExtensionManager())
}
