/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.md', which is part of this source code package.
 */

import akka.actor.ActorSystem
import akka.wamp.client.{Client, subscribe}

object FuturesScalaClient extends App {

  val system = ActorSystem()
  val client = Client(system)
  implicit val executionContext = system.dispatcher

  client.connect(endpoint = "local").map { c =>
    c.open("realm").map { implicit s =>

      subscribe("topic", (arg: Int) => {
        println(s"got $arg")
      })

//      publish("topic", List("Ciao!"))

//      call("procedure", List(20, 55)).foreach { res =>
//        res.args.foreach { args =>
//          println(s"20 * 55 = ${args(0)}")
//        }
//      }

//      register("procedure", (a: Int, b: Int) => {
//        a + b
//      })
    }
  }
}
