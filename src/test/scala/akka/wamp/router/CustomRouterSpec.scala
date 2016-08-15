package akka.wamp.router

import akka.actor.ActorSystem
import akka.wamp._
import akka.wamp.messages._
import com.typesafe.config.ConfigFactory

// it tests a custom router configuration
class CustomRouterSpec extends RouterFixtureSpec(ActorSystem("test",
  ConfigFactory.parseString(
    """
      | akka {
      |   wamp {
      |     router {
      |       auto-create-realms = true
      |     }
      |   }
      | }
    """.stripMargin)
)) {

  "The router" should "auto-create realm if client says HELLO for unknown realm" in { f =>
    f.router ! Hello("myapp.realm", Dict().withRoles("publisher"))
    expectMsgType[Welcome]
    f.router.underlyingActor.realms must have size(2)
    f.router.underlyingActor.realms must contain allOf ("akka.wamp.realm", "myapp.realm")
  }

  
}
