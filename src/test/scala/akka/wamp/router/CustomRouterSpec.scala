package akka.wamp.router

import akka.actor.ActorSystem
import akka.wamp.Dict
import akka.wamp.messages.{Abort, Hello}
import com.typesafe.config.ConfigFactory

// it tests a custom router configuration
class CustomRouterSpec extends RouterSpec(ActorSystem("test",
  ConfigFactory.parseString(
    """
      | akka {
      |   wamp {
      |     router {
      |       auto-create-realms = false
      |     }
      |   }
      | }
    """.stripMargin)
)) {

  it should "reply ABORT if client says HELLO for unknown realm" in { f =>
    f.router ! Hello("unknown.realm")
    expectMsg(Abort("wamp.error.no_such_realm", Dict("message" -> "The realm unknown.realm does not exist.")))
    f.router.underlyingActor.realms must have size(1)
    f.router.underlyingActor.realms must contain only ("akka.wamp.realm")
    f.router.underlyingActor.sessions mustBe empty
  }
}
