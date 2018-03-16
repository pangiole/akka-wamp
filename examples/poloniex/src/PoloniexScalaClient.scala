import akka.actor.ActorSystem
import akka.wamp.client.Client

object PoloniexScalaClient extends App {

  val actorSystem = ActorSystem()
  val client = Client(actorSystem)
  implicit val executionContext = actorSystem.dispatcher

  client.connect("wss://api.poloniex.com", "json").map ( conn =>
    conn.open("realm1").map { implicit session =>

      session.subscribe("ticker", event => {
        println(s"${event.kwargs} -> ${event.args}")
      })
    }
  )
}
