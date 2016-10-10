package akka.wamp.router

import com.typesafe.config.ConfigFactory


object StandaloneRouterApp extends  App {
  import akka.actor.{Scope => _, _}
  import akka.io._
  import akka.wamp._
  import akka.wamp.messages._

  val configFile = System.getProperty("config.file")
  val config = 
    if (configFile == null) 
      ConfigFactory.load()
    else
      ConfigFactory.load(configFile)

  val properties = ConfigFactory.systemProperties()
  val system = ActorSystem("wamp", properties.withFallback(config))
  system.actorOf(Props(new Binder()), name = "binder")

  class Binder extends Actor with ActorLogging {
    implicit val system = context.system
    implicit val ec = context.system.dispatcher

    val router = context.system.actorOf(Router.props(), "router")
    IO(Wamp) ! Bind(router)

    def receive = {
      case signal @ Bound(listener, url) =>
        log.info("[{}]    Successfully bound on {}", self.path.name, url)

      case CommandFailed(cmd, cause) =>
        context.system.terminate().map[Unit](_ => System.exit(-1))

    }
  }
}
