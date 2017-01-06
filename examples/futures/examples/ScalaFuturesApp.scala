package examples

import akka.actor._
import akka.wamp.client._

object ScalaFuturesApp extends App {

  val system = ActorSystem()
  val client = Client(system)
  implicit val executionContext = system.dispatcher

  client.connect(transport = "default").map { c =>
    c.open(realm = "hello").map { implicit s =>

      subscribe("mytopic", (arg: Int) => {
        println(s"got $arg")
      })

      publish("mytopic", List("Ciao!"))

      call("myproc", List(20, 55)).foreach { res =>
        res.args.foreach { args =>
          println(s"20 * 55 = ${args(0)}")
        }
      }
      
      register("myproc", (a: Int, b: Int) => {
        a + b
      })
    }
  }
}
