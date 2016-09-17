package akka.wamp.router

import akka.wamp._
import akka.wamp.messages._

import scala.concurrent.duration._

/**
  * It tests the Router running with its default settings 
  * (when NO custom configuration is applied)
  */
class RouterSpec extends RouterFixtureSpec {

  "The default router"  should "auto-create realm if client says HELLO for unknown realm" in { f =>
    f.router ! Hello("myapp.realm", Dict().addRoles("publisher"))
    expectMsgType[Welcome]
    f.router.underlyingActor.realms must have size(2)
    f.router.underlyingActor.realms must contain allOf ("akka.wamp.realm", "myapp.realm")
  }
  
  it should "reply WELCOME if client says HELLO for existing realm" in { f =>
    f.router ! Hello("akka.wamp.realm", Dict().addRoles("publisher"))
    expectMsg(Welcome(1, Dict().addRoles("broker", "dealer").setAgent("akka-wamp-0.7.0")))
    f.router.underlyingActor.realms must have size(1)
    f.router.underlyingActor.realms must contain only ("akka.wamp.realm")
    f.router.underlyingActor.sessions must have size(1)
    val session = f.router.underlyingActor.sessions(1)
    session must have (
      'id (1),
      'client (testActor),
      'roles (Set("publisher")),
      'realm ("akka.wamp.realm")
    )
  }


  it should "fail connection if client says HELLO twice (regardless the realm)" in { f =>
    // TODO https://github.com/angiolep/akka-wamp/issues/21
    pending
    f.router ! Hello("akka.wamp.realm", Dict().addRoles("publisher"))
    receiveOne(0.seconds)
    f.router ! Hello("whatever.realm", Dict().addRoles("publisher"))
    expectMsg(???)
    f.router.underlyingActor.sessions  mustBe empty
  }


  it should "fail connection if client says GOODBYE without session" in { f =>
    // TODO https://github.com/angiolep/akka-wamp/issues/22
    pending
    f.router ! Goodbye()
    expectMsg(???)
  }


  it should "reply GOODBYE if client says GOODBYE within session" in { f =>
    f.router ! Hello("akka.wamp.realm", Dict().addRoles("publisher"))
    expectMsgType[Welcome]
    f.router ! Goodbye(Dict("message" -> "The host is shutting down now."), "wamp.error.system_shutdown")
    expectMsg(Goodbye(Goodbye.defaultDetails, "wamp.error.goodbye_and_out"))
    f.router.underlyingActor.sessions  mustBe empty
  }
}
