package akka.wamp.router

import akka.wamp._
import akka.wamp.messages._

import scala.concurrent.duration._

/**
  * Test the router actor when configured with custom settings
  */
class CustomRouterSpec extends CustomSpec {
  
  "A router configured with custom settings" should "drop incoming repeated HELLOs, and resume" in { f =>
    f.router ! Hello("default", Dict().withRoles("publisher")); receiveOne(0.seconds)
    f.router.underlyingActor.sessions must have size(1)
    f.router ! Hello("default", Dict().withRoles("subscriber"))
    expectNoMessage(0.seconds)
    f.router.underlyingActor.sessions must have size(1)
  }

  it should "drop incoming repeated GOODBYEs if peer didn't open session, and resume" in { f =>
    f.router ! Goodbye()
    f.router ! Goodbye()
    f.router ! Hello()
    expectMsgType[Welcome]
    f.router.underlyingActor.sessions must have size(1)
  }
  
  it should "NOT open session on incoming HELLO('unknown.realm') and reply ABORT" in { f =>
    f.router ! Hello("unknown.realm")
    expectMsg(Abort(Dict("message"->"The realm 'unknown.realm' does not exist."), "wamp.error.no_such_realm"))
    f.router.underlyingActor.realms must have size(1)
    f.router.underlyingActor.realms must contain only ("default")
    f.router.underlyingActor.sessions mustBe empty
  }
}
