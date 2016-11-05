package examples

import akka.actor._
import akka.wamp.client._

object ScalaFuturesApp extends App {
  val system = ActorSystem()
  val client = Client(system)
  implicit val executionContext = system.dispatcher

  client.connect(transport = "default").map { conn =>
    conn.open(realm = "hello").map { implicit ssn =>

      /*subscribe("mytopic", (c: Int) => {
        println(s"got $c")
      })*/

      ssn.publish("mytopic", List("Ciao!"))

      /*register("myprocedure", (a: Int, b: Int) => {
        a + b
      })*/

      ssn.call("myprocedure", List(20, 55)).foreach { res =>
        res.args.foreach { args =>
          println(s"20 * 55 = ${args(0)}")
        }
      }
    }
  }
}
