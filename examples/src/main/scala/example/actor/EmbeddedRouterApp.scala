package example.actor

object EmbeddedRouterApp extends App {
  import akka.actor._
  import akka.io._
  import akka.wamp._
  import akka.wamp.router._

  implicit val system = ActorSystem()
  system.actorOf(Props[Binder])
  
  
  class Binder extends Actor with ActorLogging {

    override def preStart(): Unit = {
      val router = system.actorOf(Router.props(), "router")
      IO(Wamp) ! Wamp.Bind(router)  
    }
    
    override def receive: Receive = {
      case signal @ Wamp.CommandFailed(cmd, ex) =>
        log.info(s"$cmd failed because of $ex")

      case signal @ Wamp.Bound(listener, url) =>
        log.info(s"Bound succeeded with $url")
        // ...
        listener ! Wamp.Unbind
    }
  }
}
