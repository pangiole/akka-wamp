package akka.wamp.router

import akka.wamp._
import akka.wamp.messages._
import scala.concurrent.duration._

/**
  * It tests the Router running with its default settings 
  * (when NO custom configuration is applied)
  */
class RouterSpec extends RouterBaseSpec {

  "The default router" should "auto-create realms on incoming HELLO('myapp.realm') and reply GOODBYE" in { f =>
    f.router ! Hello("myapp.realm", Dict().addRoles("subscriber", "callee"))
    expectMsg(Welcome(1, Dict().addRoles("broker", "dealer").setAgent("akka-wamp-0.12.0")))
    f.router.underlyingActor.realms must have size(2)
    f.router.underlyingActor.realms must contain allOf ("default.realm", "myapp.realm")
    f.router.underlyingActor.sessions.values.loneElement must have (
      'id (1),
      'peer (testActor),
      'roles (Set("subscriber", "callee")),
      'realm ("myapp.realm")
    )
  }

  it should "disconnect on incoming repeated HELLOs('whatever.realm')" in { f =>
    f.router ! Hello("default.realm", Dict().addRoles("publisher")); receiveOne(0.seconds)
    f.router.underlyingActor.sessions must have size(1)
    f.router ! Hello("whatever.realm", Dict().addRoles("subscriber"))
    expectMsg(Disconnect)
    f.router.underlyingActor.sessions  mustBe empty
  }

  it should "disconnect on incoming GOODBYE if peer didn't open session" in { f =>
    f.router ! Goodbye()
    expectMsg(Disconnect)
  }

  
  it should "open session on incoming HELLO('default.realm') and reply WELCOME" in { f =>
    f.router ! Hello("default.realm", Dict().addRoles("publisher"))
    expectMsg(Welcome(1, Dict().addRoles("broker", "dealer").setAgent("akka-wamp-0.12.0")))
    f.router.underlyingActor.realms must have size(1)
    f.router.underlyingActor.realms must contain only ("default.realm")
    f.router.underlyingActor.sessions.values.loneElement must have (
      'id (1),
      'peer (testActor),
      'roles (Set("publisher")),
      'realm ("default.realm")
    )
  }
  

  it should "close session on incoming GOODBYE and reply GOODBYE" in { f =>
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
