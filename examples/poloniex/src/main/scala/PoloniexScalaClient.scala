import akka.actor._
import akka.wamp.client._

object PoloniexScalaClient extends App {

  val actorSystem = ActorSystem()
  val client = Client(actorSystem)
  implicit val executionContext = actorSystem.dispatcher

  client.connect("wss://api.poloniex.com", "json").map ( conn =>
    conn.open("realm1").map { implicit session =>

      session.subscribe("BTC_XMR", event => {
        println(s"${event.kwargs} -> ${event.args}")
      })
    }
  )
}

