package akka.wamp.router

import akka.actor.ActorSystem
import akka.wamp._
import akka.wamp.messages._
import com.typesafe.config.ConfigFactory

/**
  * It tests the Router running with some custom configuration
  * (e.g. when it shall auto create realms)
  */
class CustomRouterSpec extends RouterFixtureSpec(ActorSystem("test",
  ConfigFactory.parseString(
    """
      | akka {
      |   wamp {
      |     serializing {
      |       validate-strict-uris = true
      |     }
      |     router {
      |       abort-unknown-realms = true
      |     }
      |   }
      | }
    """.stripMargin)
)) {

  "A router with custom settings" should "reply ABORT if client says HELLO for unknown realm" in { fixture =>
    fixture.router ! Hello("unknown.realm")
    expectMsg(Abort(Dict("message" -> "The realm unknown.realm does not exist."), "wamp.error.no_such_realm"))
    fixture.router.underlyingActor.realms must have size(1)
    fixture.router.underlyingActor.realms must contain only ("akka.wamp.realm")
    fixture.router.underlyingActor.sessions mustBe empty
  }
  
  // TODO test some behaviours involving strict URI validation
}
