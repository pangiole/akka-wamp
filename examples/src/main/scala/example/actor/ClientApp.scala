package example.actor

/**
  * Example of client app written with Actor based API
  */
object ClientApp extends App {
  import akka.actor._
  import akka.io._
  import akka.wamp._

  implicit val system = ActorSystem()
  system.actorOf(Props[ClientActor])

  class ClientActor extends Actor {
    val manager = IO(Wamp)
    manager ! Wamp.Connect("ws://localhost:8080/router", "wamp.2.json")
    
    var conn: ActorRef = _
    
    override def receive = {
      case signal @ Wamp.Connected(c) =>
        conn = c
    }
  }
  
}