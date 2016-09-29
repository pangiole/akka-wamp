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
    expectMsg(Welcome(1, Dict().addRoles("broker", "dealer").setAgent("akka-wamp-0.9.0")))
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
    expectMsg(Abort(reason = "akka.wamp.error.session_already_open"))
    f.router.underlyingActor.sessions mustBe empty
  }

  
  // TODO https://github.com/angiolep/akka-wamp/issues/22
  it should "drop message GOODBYE if it didn't open session" in { f =>
    f.router ! Goodbye()
    f.router ! Hello()
    expectMsgType[Welcome]
    f.router.underlyingActor.sessions must have size(1)
  }
  
  
  it should "open session and reply WELCOME on HELLO for existing realm" in { f =>
    f.router ! Hello("akka.wamp.realm", Dict().addRoles("publisher"))
    expectMsg(Welcome(1, Dict().addRoles("broker", "dealer").setAgent("akka-wamp-0.9.0")))
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
    
    f.router ! Subscribe(1, topic = "myapp.topic")
    f.router.underlyingActor.subscriptions must have size(1)
    expectMsgType[Subscribed]
    
    f.router ! Register(2, procedure = "myapp.procedure")
    f.router.underlyingActor.registrations must have size(1)
    expectMsgType[Registered]
    
    f.router ! Goodbye()
    expectMsg(Goodbye(reason = "wamp.error.goodbye_and_out"))
    f.router.underlyingActor.sessions mustBe empty
    f.router.underlyingActor.subscriptions mustBe empty
    f.router.underlyingActor.registrations mustBe empty
  }
}
