package akka.wamp.router

import akka.wamp._
import akka.wamp.messages._
import scala.concurrent.duration._

/**
  * It tests the Router running with its default settings 
  * (when NO custom configuration is applied)
  */
class RouterSpec extends RouterFixtureSpec {

  "The default router" should "auto-create realm and reply GOODBYE on HELLO for new.realm" in { f =>
    f.router ! Hello("new.realm", Dict().addRoles("subscriber", "callee"))
    expectMsg(Welcome(1, Dict().addRoles("broker", "dealer").setAgent("akka-wamp-0.7.0")))
    f.router.underlyingActor.realms must have size(2)
    f.router.underlyingActor.realms must contain allOf ("akka.wamp.realm", "new.realm")
    f.router.underlyingActor.sessions.values.loneElement must have (
      'id (1),
      'peer (testActor),
      'roles (Set("subscriber", "callee")),
      'realm ("new.realm")
    )
  }

  // TODO https://github.com/angiolep/akka-wamp/issues/21
  it should "fail session on HELLO twice (regardless the realm)" in { f =>
    f.router ! Hello("akka.wamp.realm", Dict().addRoles("publisher")); receiveOne(0.seconds)
    f.router.underlyingActor.sessions must have size(1)
    f.router ! Hello("akka.wamp.realm", Dict().addRoles("subscriber"))
    expectMsg(Goodbye(Dict("message"->"Second HELLO message received during the lifetime of the session"), "akka.wamp.error.session_failure"))
    f.router.underlyingActor.sessions mustBe empty
  }

  // TODO https://github.com/angiolep/akka-wamp/issues/22
  it should "drop message and resume on GOODBYE before open session" in { f =>
    f.router ! Goodbye()
    f.router ! Hello()
    expectMsgType[Welcome]
    f.router.underlyingActor.sessions must have size(1)
  }
  
  it should "open session and reply WELCOME on HELLO for existing realm" in { f =>
    f.router ! Hello("akka.wamp.realm", Dict().addRoles("publisher"))
    expectMsg(Welcome(1, Dict().addRoles("broker", "dealer").setAgent("akka-wamp-0.7.0")))
    f.router.underlyingActor.realms must have size(1)
    f.router.underlyingActor.realms must contain only ("akka.wamp.realm")
    f.router.underlyingActor.sessions.values.loneElement must have (
      'id (1),
      'peer (testActor),
      'roles (Set("publisher")),
      'realm ("akka.wamp.realm")
    )
  }
  

  it should "close session and reply GOODBYE on GOODBYE" in { f =>
    f.router ! Hello()
    expectMsgType[Welcome]
    f.router.underlyingActor.sessions must have size(1)
    f.router ! Goodbye()
    expectMsg(Goodbye(reason = "wamp.error.goodbye_and_out"))
    f.router.underlyingActor.sessions mustBe empty
  }
}
