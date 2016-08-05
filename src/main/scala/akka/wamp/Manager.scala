package akka.wamp

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.wamp.client.ClientManager
import akka.wamp.router.RouterManager


private[wamp] class Manager()(implicit system: ActorSystem, mat: ActorMaterializer) extends Actor with ActorLogging {

  // router -> binding
  //var bindings = Map.empty[ActorRef, Future[Http.ServerBinding]]
  
  override def receive: Receive = {
    case cmd: Wamp.Bind => {
      val routeManager = context.actorOf(Props(new RouterManager))
      routeManager.forward(cmd)
    }
      
    // case Wamp.Unbind =>
    //  val router = sender()
    //  for { binding <- bindings(router) } yield (binding.unbind())
      
    case cmd: Wamp.Connect => 
      val clientManager = context.actorOf(Props(new ClientManager))
      clientManager.forward(cmd)
  }
}


object Manager {
  def props()(implicit system: ActorSystem, mat: ActorMaterializer) = Props(new Manager())
}
