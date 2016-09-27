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
  // TODO https://github.com/angiolep/akka-wamp/issues/33
  // TODO write a CustomDealerSpec to expect disconnections before open session
  "The default dealer" should "drop REGISTER if peer didn't open session" in { f =>
    f.client.send(f.router, Register(1, procedure = "myapp.procedure"))
    f.client.send(f.router, Hello())
    f.client.expectMsgType[Welcome]
    f.router.underlyingActor.sessions must have size(1)
  }

  
  it should "drop REGISTER if peer didn't announce 'callee' role" in { f =>
    f.client.send(f.router, Hello(details = Dict().addRoles("publisher")))
    f.client.expectMsgType[Welcome]
    f.router.underlyingActor.registrations mustBe empty
    
    f.client.send(f.router, Register(1, procedure = "myapp.procedure"))
    f.client.expectNoMsg()
    f.router.underlyingActor.registrations mustBe empty
  }


  it should "create new registration on REGISTER from callee" in { f =>
    f.client.send(f.router, Hello(details = Dict().addRoles("callee")))
    f.client.expectMsgType[Welcome]
    f.router.underlyingActor.registrations mustBe empty
    
    f.client.send(f.router, Register(44, procedure = "mypp.procedure"))
    f.client.receiveOne(0.seconds) match {
      case Registered(requestId, registrationId) =>
        requestId mustBe 44
        f.router.underlyingActor.registrations must have size(1)
        f.router.underlyingActor.registrations(registrationId) must have (
          'id (registrationId),
          'callee (f.client.ref),
          'procedure ("mypp.procedure")
        )
      case m => fail(s"unexpected $m")
    }
  }


  it should "confirm existing registration on repeated REGISTER same procedure from same callee" in { f =>
    f.client.send(f.router, Hello(details = Dict().addRoles("callee")))
    f.client.expectMsgType[Welcome]
    f.router.underlyingActor.registrations mustBe empty
    
    f.client.send(f.router, Register(44, procedure = "mypp.procedure"))
    f.client.expectMsgType[Registered]
    f.router.underlyingActor.registrations must have size(1)
    
    f.client.send(f.router, Register(55, procedure = "mypp.procedure"))
    f.client.receiveOne(0.seconds) match {
      case Registered(requestId, registrationId) =>
        requestId mustBe 55
        f.router.underlyingActor.registrations must have size(1)
        f.router.underlyingActor.registrations(registrationId) must have (
          'id (registrationId),
          'callee (f.client.ref),
          'procedure ("mypp.procedure")
        )
      case m => fail(s"unexpected $m")
    }
  }


  it should "create new registration2 on REGISTER procedure2 from same client" in { f =>
    f.client.send(f.router, Hello(details = Dict().addRoles("callee")))
    f.client.expectMsgType[Welcome]
    f.router.underlyingActor.registrations mustBe empty
    
    f.client.send(f.router, Register(44, procedure = "mypp.procedure1"))
    val registrationId1 = f.client.expectMsgType[Registered].registrationId
    f.router.underlyingActor.registrations must have size(1)
    
    f.client.send(f.router, Register(55, procedure = "mypp.procedure2"))
    f.client.receiveOne(0.seconds) match {
      case Registered(requestId, registrationId2) =>
        requestId mustBe 55
        f.router.underlyingActor.registrations must have size(2)
        f.router.underlyingActor.registrations(registrationId1) must have (
          'id (registrationId1),
          'callee (f.client.ref),
          'procedure ("mypp.procedure1")
        )
        f.router.underlyingActor.registrations(registrationId2) must have (
          'id (registrationId2),
          'callee (f.client.ref),
          'procedure ("mypp.procedure2")
        )
      case m => fail(s"unexpected $m")
    }
  }


  it should "reply ERROR on REGISTER existing procedure1 from different client2" in { f =>
    val client1 = TestProbe("client1")
    client1.send(f.router, Hello(details = Dict().addRoles("callee")))
    client1.expectMsgType[Welcome]
    f.router.underlyingActor.registrations mustBe empty

    client1.send(f.router, Register(44, procedure = "mypp.procedure1"))
    client1.expectMsgType[Registered]
    f.router.underlyingActor.registrations must have size(1)
    
    val client2 = TestProbe("client2")
    client2.send(f.router, Hello(details = Dict().addRoles("callee")))
    client2.expectMsgType[Welcome]
    f.router.underlyingActor.registrations must have size(1)
    
    client2.send(f.router, Register(55, procedure = "mypp.procedure1"))
    client2.expectMsg(Error(Register.tpe, 55, error = "wamp.error.procedure_already_exists"))
    f.router.underlyingActor.registrations must have size(1)
  }

  
  // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

  it should "drop UNREGISTER if peer didn't open session" in { f =>
    f.client.send(f.router, Unregister(66, 99))
    f.client.send(f.router, Hello())
    f.client.expectMsgType[Welcome]
    f.router.underlyingActor.sessions must have size(1)
  }

  
  it should "drop UNREGISTER if peer didn't announce 'callee' role" in { f =>
    f.client.send(f.router, Hello(details = Dict().addRoles("publisher")))
    f.client.expectMsgType[Welcome]
    f.router.underlyingActor.registrations mustBe empty
    
    f.client.send(f.router, Unregister(66, 99))
    f.client.expectNoMsg()
    f.router.underlyingActor.registrations mustBe empty
  }
  
  
  it should "remove existing registration on UNREGISTER from callee" in { f =>
    f.client.send(f.router, Hello(details = Dict().addRoles("callee")))
    f.client.expectMsgType[Welcome]
    f.router.underlyingActor.registrations mustBe empty
    
    f.client.send(f.router, Register(44, procedure = "mypp.procedure"))
    val registrationId = f.client.expectMsgType[Registered].registrationId
    f.router.underlyingActor.registrations must have size(1)
    
    f.client.send(f.router, Unregister(66, registrationId))
    f.client.expectMsg(Unregistered(66))
    f.router.underlyingActor.registrations  mustBe empty
  }

  
  it should "reply ERROR on UNREGISTER unknown registration" in { f =>
    f.client.send(f.router, Hello(details = Dict().addRoles("callee")))
    f.client.expectMsgType[Welcome]
    f.router.underlyingActor.registrations mustBe empty

    f.client.send(f.router, Unregister(66, registrationId = 999))
    f.client.expectMsg(Error(Unregister.tpe, 66, error = "wamp.error.no_such_registration"))
  }


  it should "reply ERROR on attempt to UNREGISTER callee1's registration from callee2" in { f =>
    val client1 = TestProbe("client1")
    client1.send(f.router, Hello(details = Dict().addRoles("callee")))
    client1.expectMsgType[Welcome]
    f.router.underlyingActor.registrations mustBe empty

    client1.send(f.router, Register(44, procedure = "mypp.procedure"))
    val registrationId1 = client1.expectMsgType[Registered].registrationId
    f.router.underlyingActor.registrations must have size(1)

    val client2 = TestProbe("client2")
    client2.send(f.router, Hello(details = Dict().addRoles("callee")))
    client2.expectMsgType[Welcome]
    f.router.underlyingActor.registrations must have size(1)

    client2.send(f.router, Unregister(66, registrationId1))
    client2.expectMsg(Error(Unregister.tpe, 66, error = "wamp.error.no_such_registration"))
  }
  
  
  // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

  
  it should "drop CALL if peer didn't open session" in { f =>
    f.client.send(f.router, Call(444, procedure = "myapp.procedure"))
    f.client.send(f.router, Hello())
    f.client.expectMsgType[Welcome]
    f.router.underlyingActor.sessions must have size(1)
  }
  

  it should "drop CALL if peer didn't announce 'caller' role" in { f =>
    f.client.send(f.router, Hello(details = Dict().addRoles("publisher")))
    f.client.expectMsgType[Welcome]
    f.router.underlyingActor.invocations mustBe empty
    
    f.client.send(f.router, Call(444, procedure = "myapp.procedure"))
    f.client.expectNoMsg()
    f.router.underlyingActor.invocations mustBe empty
  }
  
  
  it should "send INVOCATION to callee on CALL procedure from caller" in { f =>
    val client1 = TestProbe("client1")
    client1.send(f.router, Hello(details = Dict().addRoles("caller")))
    client1.expectMsgType[Welcome]
    f.router.underlyingActor.registrations mustBe empty
    f.router.underlyingActor.invocations mustBe empty

    val client2 = TestProbe("client2")
    client2.send(f.router, Hello(details = Dict().addRoles("callee")))
    client2.expectMsgType[Welcome]
    f.router.underlyingActor.registrations mustBe empty
    f.router.underlyingActor.invocations mustBe empty
    
    client2.send(f.router, Register(1, procedure = "myapp.procedure"))
    val registrationId = client2.expectMsgType[Registered].registrationId
    f.router.underlyingActor.registrations must have size(1)
    f.router.underlyingActor.invocations mustBe empty
    
    client1.send(f.router, Call(1, procedure = "myapp.procedure"))
    client1.expectNoMsg()
    f.router.underlyingActor.registrations must have size(1)
    f.router.underlyingActor.invocations must have size(1)
    
    client2.expectMsg(Invocation(1, registrationId, Invocation.defaultDetails))
  }


  it should "drop YIELD if peer didn't open session" in { f =>
    f.client.send(f.router, Yield(1))
    f.client.send(f.router, Hello())
    f.client.expectMsgType[Welcome]
    f.router.underlyingActor.sessions must have size(1)
  }


  it should "drop YIELD if peer didn't announce 'callee' role" in { f =>
    f.client.send(f.router, Hello(details = Dict().addRoles("subscriber")))
    f.client.expectMsgType[Welcome]
    f.client.send(f.router, Yield(1))
    f.client.expectNoMsg()
  }
  
  it should "send RESULT to caller on YIELD from callee" in { f =>
    val client1 = TestProbe("caller")
    val call = Call(444, procedure = "myapp.procedure")
    f.router.underlyingActor.invocations += (888L -> new Dealer.OutstandingInvocation(client1.ref, call))
    
    val client2 = TestProbe("callee")
    client2.send(f.router, Hello(details = Dict().addRoles("callee")))
    client2.expectMsgType[Welcome]
    
    val payload = Payload(44.23,"paolo",null,true)
    client2.send(f.router, Yield(888, payload = Some(payload)))
    client1.expectMsg(Result(444, payload = Some(payload)))
  }
}
