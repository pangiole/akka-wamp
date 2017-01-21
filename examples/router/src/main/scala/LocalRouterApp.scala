/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.md', which is part of this source code package.
 */

import akka.actor._
import akka.wamp.router._

object LocalRouterApp extends App {
  val actorSystem = ActorSystem()
  EmbeddedRouter.createAndBind(actorSystem)
}
