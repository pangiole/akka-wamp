package akka.wamp.router

import com.typesafe.config.ConfigFactory

class CustomSpec extends DefaultSpec(
  ConfigFactory.parseString(
    """
      | akka {
      |   wamp {
      |     router {
      |       abort-unknown-realms         = true
      |       validate-strict-uris         = true
      |       tolerate-protocol-violations = true
      |     }
      |   }
      | }
    """.stripMargin)
)