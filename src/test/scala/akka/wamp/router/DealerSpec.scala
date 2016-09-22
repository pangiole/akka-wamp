package akka.wamp.router

import akka.testkit._
import akka.wamp._
import akka.wamp.messages._
import akka.wamp.serialization._

import scala.concurrent.duration._

/**
  * It tests the router as dealer running with its default settings 
  * (when NO custom configuration is applied).
  */
class DealerSpec extends RouterFixtureSpec {

  // TODO https://github.com/angiolep/akka-wamp/issues/22
  // TODO write a CustomDealerSpec to expect disconnections before open session
  "The default router as dealer" should "drop message and resume on REGISTER before open session" in { f =>
    f.router ! Register(1, options = Dict(), "myapp.procedure")
    f.router ! Hello()
    expectMsgType[Welcome]
    f.router.underlyingActor.sessions must have size(1)
  }



  it should "reply ERROR on REGISTER if peer did not announce 'callee' role" in { f =>
    val client = TestProbe("client")
    client.send(f.router, Hello(details = Dict().addRoles(Roles.publisher))); client.receiveOne(0.seconds)
    client.send(f.router, Register(1, procedure = "myapp.procedure"))
    client.expectMsg(Error(Register.tpe, 1, Error.defaultDetails, "akka.wamp.error.no_callee_role"))
    client.expectNoMsg()
    f.router.underlyingActor.registrations mustBe empty
  }


  it should "create new registration on first REGISTER" in { f =>
    val client = TestProbe("client")
    client.send(f.router , Hello()); client.receiveOne(0.seconds)
    client.send(f.router, Register(1, procedure = "mypp.procedure"))
    client.receiveOne(0.seconds) match {
      case Registered(requestId, registrationId) =>
        requestId mustBe 1
        f.router.underlyingActor.registrations must have size(1)
        f.router.underlyingActor.registrations(registrationId) must have (
          'id (registrationId),
          'callee (client.ref),
          'procedure ("mypp.procedure")
        )
      case m => fail(s"unexpected $m")
    }
  }


  it should "confirm existing registration on repeated REGISTER same procedure from same client" in { f =>
    val client = TestProbe("client")
    client.send(f.router , Hello()); client.receiveOne(0.seconds)
    client.send(f.router, Register(1, procedure = "mypp.procedure")); client.receiveOne(0.seconds)
    client.send(f.router, Register(2, procedure = "mypp.procedure"))
    client.receiveOne(0.seconds) match {
      case Registered(requestId, registrationId) =>
        requestId mustBe 2
        f.router.underlyingActor.registrations must have size(1)
        f.router.underlyingActor.registrations(registrationId) must have (
          'id (registrationId),
          'callee (client.ref),
          'procedure ("mypp.procedure")
        )
      case m => fail(s"unexpected $m")
    }
  }


  it should "create new registration2 on REGISTER procedure2 from same client" in { f =>
    val client = TestProbe("client")
    client.send(f.router, Hello()); client.receiveOne(0.seconds)
    client.send(f.router, Register(1, procedure = "mypp.procedure1"))
    val id1 = client.receiveOne(0.seconds).asInstanceOf[Registered].registrationId
    client.send(f.router, Register(2, procedure = "mypp.procedure2"))
    client.receiveOne(0.seconds) match {
      case Registered(_, id2) =>
        f.router.underlyingActor.registrations must have size(2)
        f.router.underlyingActor.registrations(id1) must have (
          'id (id1),
          'callee (client.ref),
          'procedure ("mypp.procedure1")
        )
        f.router.underlyingActor.registrations(id2) must have (
          'id (id2),
          'callee (client.ref),
          'procedure ("mypp.procedure2")
        )
      case m => fail(s"unexpected $m")
    }
  }


  it should "reply ERROR on REGISTER existing procedure1 from different client2" in { f =>
    val client1 = TestProbe("client1")
    client1.send(f.router, Hello()); client1.receiveOne(0.seconds)
    client1.send(f.router, Register(1, procedure = "mypp.procedure1")); client1.receiveOne(0.second)
    
    val client2 = TestProbe("client2")
    client2.send(f.router, Hello()); client2.receiveOne(0.seconds)
    client2.send(f.router, Register(2, procedure = "mypp.procedure1"))
    client2.expectMsg(Error(Register.tpe, 2, Error.defaultDetails, "wamp.error.procedure_already_exists"))
    client2.expectNoMsg()
    f.router.underlyingActor.registrations must have size(1)
  }

  
  it should "remove existing registration on UNREGISTER" in { f =>
    val client = TestProbe("client")
    client.send(f.router , Hello()); client.receiveOne(0.seconds)
    client.send(f.router, Register(1, procedure = "mypp.procedure"))
    val registrationId = client.receiveOne(0.seconds).asInstanceOf[Registered].registrationId
    client.send(f.router, Unregister(2, registrationId))
    client.expectMsg(Unregistered(2))
    f.router.underlyingActor.registrations  mustBe empty
  }

  
  it should "reply ERROR on UNREGISTER unknown registration" in { f =>
    val client1 = TestProbe("client1")
    client1.send(f.router , Hello()); client1.receiveOne(0.seconds)
    client1.send(f.router, Register(1, procedure = "mypp.procedure"))
    val registrationId1 = client1.receiveOne(0.seconds).asInstanceOf[Registered].registrationId
    
    val client2 = TestProbe("client2")
    client2.send(f.router , Hello()); client2.receiveOne(0.seconds)
    
    client2.send(f.router, Unregister(1, registrationId1))
    client2.expectMsg(Error(Unregister.tpe, 1, error = "wamp.error.no_such_registration"))

    client2.send(f.router, Unregister(2, registrationId = 999))
    client2.expectMsg(Error(Unregister.tpe, 2, error = "wamp.error.no_such_registration"))
  }
  
  
  
  // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

  
  it should "drop message and resume on CALL before open session" in { f =>
    pending
//    f.router ! Call(1, "myapp.procedure")
//    f.router ! Hello()
//    expectMsgType[Welcome]
//    f.router.underlyingActor.sessions must have size(1)
  }
  

  it should "reply ERROR on CALL if peer did not announce 'caller' role" in { f =>
    pending
//    val client = TestProbe("client")
//    client.send(f.router, Hello(details = Dict().addRoles(Roles.publisher))); client.receiveOne(0.seconds)
//    client.send(f.router, Call(1, "myapp.procedure"))
//    client.expectMsg(Error(Call.tpe, 1, Error.defaultDetails, "akka.wamp.error.no_caller_role"))
//    client.expectNoMsg()
//    f.router.underlyingActor.calls mustBe empty
  }
}
