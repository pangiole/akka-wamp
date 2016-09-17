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

  "The default router as dealer" should "fail connection on REGISTER but no session open" in { f =>
    // TODO https://github.com/angiolep/akka-wamp/issues/22
    pending
    f.router ! Register(1, options = Dict(), "myapp.procedure")
    expectMsg(???)
  }



  it should "reply ERROR on REGISTER but no 'callee' role in session" in { f =>
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


  it should "confirm existing registration on repeated REGISTER the same procedure" in { f =>
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


  it should "create new registration2 on REGISTER to procedure2 from same client" in { f =>
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


  it should "reply ERROR on REGISTER existing procedure" in { f =>
    val client1 = TestProbe("client1")
    client1.send(f.router, Hello()); client1.receiveOne(0.seconds)
    client1.send(f.router, Register(1, procedure = "mypp.procedure")); client1.receiveOne(0.second)
    
    val client2 = TestProbe("client2")
    client2.send(f.router, Hello()); client2.receiveOne(0.seconds)
    client2.send(f.router, Register(2, procedure = "mypp.procedure"))
    client2.expectMsg(Error(Register.tpe, 2, Error.defaultDetails, "wamp.error.procedure_already_exists"))
    client2.expectNoMsg()
    f.router.underlyingActor.registrations must have size(1)
  }

  
  it should "remove existing registration on UNREGISTER" in { f =>
    val client = TestProbe("client1")
    client.send(f.router , Hello()); client.receiveOne(0.seconds)
    client.send(f.router, Register(1, procedure = "mypp.procedure"))
    val registrationId = client.receiveOne(0.seconds).asInstanceOf[Registered].registrationId
    client.send(f.router, Unregister(2, registrationId))
    client.expectMsg(Unregistered(2))
    f.router.underlyingActor.registrations  mustBe empty
  }

  it should "reply ERROR on UNREGISTER unknown registration" in { f =>
    val client = TestProbe("client1")
    client.send(f.router , Hello()); client.receiveOne(0.seconds)
    client.send(f.router, Unregister(1, 9999))
    client.expectMsg(Error(Unregister.tpe, 1, Error.defaultDetails, "wamp.error.no_such_registration"))
  }
}
