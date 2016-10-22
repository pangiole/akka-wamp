package akka.wamp.router

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory

class CustomRouterBaseSpec extends RouterBaseSpec(ActorSystem("test",
  ConfigFactory.parseString(
    """
      | akka {
      |   wamp {
      |     router {
      |       abort-unknown-realms    = true
      |       validate-strict-uris    = true
      |       drop-offending-messages = true
      |     }
      |   }
      | }
    """.stripMargin)
))