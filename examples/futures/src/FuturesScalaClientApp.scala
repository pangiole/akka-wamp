
import akka.actor._
import akka.wamp.client._
import akka.wamp.messages._


object FuturesScalaClientApp extends App {

  val endpoint = if (args.length > 0) args(0) else "default"

  val system = ActorSystem()
  val client = Client(system)
  implicit val executionContext = system.dispatcher

  // Note that you won't need to explicitly pass in default endpoint and default realm
  client.connect(endpoint).map { conn =>
    conn.open(realm = "default").map { sess =>

      sess.subscribe("topic", consumer = (evt: Event) => {
        println(s"got $evt")
      })

      sess.publish("topic", List("Ciao!"))

      sess.register("procedure", handler = (inv: Invocation) => {
        val args = inv.args
        val a = args(0).asInstanceOf[Int]
        val b = args(1).asInstanceOf[Int]
        a + b
      })

      sess.call("procedure", List(20, 55)).foreach { res =>
        println(s"20 + 55 = ${res.args(0)}")

        // Finally WAMP disconnect, terminate Akka and exit the JVM
        conn.disconnect()
        system.terminate().foreach(_ => System.exit(0))
      }
    }
  }
}
