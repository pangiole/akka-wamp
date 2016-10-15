package examples

object FutureBasedClientApp extends App {

  import akka.wamp.client._
  val client = Client()

  implicit val ec = client.executionContext

  for {
    connection <- client
      .connect(
        url = "ws://localhost:8080/router",
        subprotocol = "wamp.2.json")
    session <- connection
      .openSession(
        realm = "default.realm",
        roles = Set("subscriber"))
    subscription <- session
      .subscribe(
        topic = "myapp.topic1")(
        event =>
          event.data.map { data =>
            println(data)
            if (data(0).toString == "Everybody out!") {
              connection.disconnect().map(d => System.exit(0))
            }
          }
      )
    publication <- session
      .publish(
        topic = "myapp.topic2",
        ack = false,
        kwdata = Map("name" -> "paolo", "age" -> 40)
      )
  } yield ()
}
