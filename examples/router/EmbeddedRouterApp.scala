object EmbeddedRouterApp extends App {

  import akka.actor._
  import akka.io._
  import akka.wamp._
  import akka.wamp.router._
  import akka.wamp.messages._

  implicit val system = ActorSystem()
  system.actorOf(Props[Binder])

  /*
   * 1. Spawn an embedded Router
   * 2. Bind it to the Wamp extension manager
   * 3. Receive the Bound signal
   */
  class Binder extends Actor with ActorLogging {

    override def preStart(): Unit = {
      val router = system.actorOf(Router.props(), "router")
      val manager = IO(Wamp)
      manager ! Bind(router)
    }

    override def receive: Receive = {
      case signal @ CommandFailed(cmd, ex) =>
        log.warning(s"$cmd failed because of $ex")

      case signal @ Bound(listener, url) =>
        log.debug(s"Bound to $url")
        // ...
        // listener ! Unbind
    }
  }

}
