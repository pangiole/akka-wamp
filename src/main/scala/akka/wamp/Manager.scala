package akka.wamp

import akka.actor.{Actor, ActorLogging, Props}


private[wamp] class Manager extends Actor with ActorLogging {

  // router -> binding
  //var bindings = Map.empty[ActorRef, Future[Http.ServerBinding]]
  
  override def receive: Receive = {
    case cmd: Wamp.Bind => {
      val manager = context.actorOf(router.Manager.props())
      manager.forward(cmd)
    }
      
    // case Wamp.Unbind =>
    //  val router = sender()
    //  for { binding <- bindings(router) } yield (binding.unbind())
      
    case cmd: Wamp.Connect => 
      val manager = context.actorOf(client.Manager.props())
      manager.forward(cmd)
  }
}


private[wamp] object Manager {
  def props() = Props(new Manager())
}
