
import akka.actor._
import akka.wamp.client._

object FuturesScalaClient extends App {

  val system = ActorSystem()
  val client = Client(system)
  implicit val executionContext = system.dispatcher

  client.connect(endpoint = "local").map { c =>
    c.open("realm").map { implicit s =>

      subscribe("topic", (arg: Int) => {
        println(s"got $arg")
      })

      publish("topic", List("Ciao!"))

      call("procedure", List(20, 55)).foreach { res =>
        println(s"20 * 55 = ${res.args(0)}")
      }

      register("procedure", (a: Int, b: Int) => {
        a + b
      })
    }
  }
}
