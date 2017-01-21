
/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.md', which is part of this source code package.
 */

import akka.actor._
import akka.wamp.client._

object FuturesSecuredClient extends App {
  val system = ActorSystem("client")
  val client = Client(system)
  implicit val executionContext = system.dispatcher

  client.connect(endpoint = "secured").map { c =>
    c.open("realm").map { implicit s =>

      subscribe("topic", (greet: String) =>
        println(greet)
      )
    }
  }
}
