/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.md', which is part of this source code package.
 */

import akka.actor.ActorSystem
import akka.wamp.router.EmbeddedRouter
import com.typesafe.config.ConfigFactory

object SecuredRouterApp extends App {
  val config = ConfigFactory.load("secured.conf")
  val actorSystem = ActorSystem("secured", config)
  EmbeddedRouter.createAndBind(actorSystem)
}
