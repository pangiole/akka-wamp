
import akka.actor._
import akka.wamp.client._
import akka.wamp.macros._

object MacrosScalaClient extends App {

  val system = ActorSystem()
  val log = system.log
  val client = Client(system)
  implicit val executionContext = system.dispatcher

  client.connect(endpoint = "default").map { conn =>
    conn.open("realm").map { implicit sess =>

      subscribe("topic", (arg: Int) => {
        log.debug(s"Received event with arg=$arg")
      })

      publish("topic", List("Ciao!"))

      register("add", (a: Int, b: Int) => {
        log.debug(s"Received invocation with args=($a, $b)")
        a + b
      })

      call("add", List(20, 55)).foreach { res =>
        log.debug(s"Received result with arg(0)=${res.args(0)}")

        conn.disconnect()
        system.terminate.foreach(_ => System.exit(0))
      }
    }
  }
}
