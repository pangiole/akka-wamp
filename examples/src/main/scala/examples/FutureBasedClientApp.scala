package examples

object FutureBasedClientApp extends App {

  import akka.wamp.client._
  val client = Client()

  implicit val ec = client.executionContext

  for {
    session <- client
      .openSession(
        url = "ws://localhost:8080/router",
        subprotocol = "wamp.2.json",
        realm = "akka.wamp.realm",
        roles = Set("subscriber"))
    subscription <- session
      .subscribe(
        topic = "myapp.topic1")(
        event =>
          event.data.map(println)
      )
    publication <- session
      .publish(
        topic = "myapp.topic2",
        ack = false,
        kwdata = Map("name" -> "paolo", "age" -> 40)
      )
  } yield ()
}
