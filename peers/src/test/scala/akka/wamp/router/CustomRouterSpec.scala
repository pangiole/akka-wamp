package akka.wamp.router

import akka.actor.ActorSystem
import akka.wamp._
import akka.wamp.messages._
import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._

/**
  * It tests the Router running with some custom configuration
  * (e.g. when it shall auto create realms)
  */
class CustomRouterSpec extends RouterFixtureSpec(ActorSystem("test",
  ConfigFactory.parseString(
    """
      | akka {
      |   wamp {
      |     router {
      |       abort-unknown-realms = true
      |       validate-strict-uris = true
      |       disconnect-offending-peers = true
      |     }
      |   }
      | }
    """.stripMargin)
)) {

  "A router with custom settings" should "reply ABORT on HELLO for unknown realm" in { f =>
    f.router ! Hello("unknown.realm")
    expectMsg(Abort(Dict("message"->"The realm 'unknown.realm' does not exist."), "wamp.error.no_such_realm"))
    f.router.underlyingActor.realms must have size(1)
    f.router.underlyingActor.realms must contain only ("akka.wamp.realm")
    f.router.underlyingActor.sessions mustBe empty
  }


  it should "disconnect on HELLO twice (regardless the realm)" in { f =>
    f.router ! Hello("akka.wamp.realm", Dict().addRoles("publisher")); receiveOne(0.seconds)
    f.router.underlyingActor.sessions must have size(1)
    f.router ! Hello("whatever.realm", Dict().addRoles("subscriber"))
    expectMsg(Disconnect)
    f.router.underlyingActor.sessions  mustBe empty
  }

  it should "disconnect on GOODBYE before open session" in { f =>
    f.router ! Goodbye()
    expectMsg(Disconnect)
  }
}
