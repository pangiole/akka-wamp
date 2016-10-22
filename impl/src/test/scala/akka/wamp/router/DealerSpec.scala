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
class DealerSpec extends RouterBaseSpec {

  "The default dealer" should "disconnect on incoming REGISTER if peer didn't open session" in { f =>
    f.router ! Register(1, procedure = "myapp.procedure")
    expectMsg(Disconnect)
    f.router.underlyingActor.sessions  mustBe empty
    f.router.underlyingActor.registrations  mustBe empty
  }
  

  it should "create new registration on incoming REGISTER from callee" in { f =>
    f.router ! Hello(details = Dict().addRoles("callee"))
    expectMsgType[Welcome]
    f.router.underlyingActor.registrations mustBe empty
    
    f.router ! Register(44, procedure = "mypp.procedure")
    val registered = expectMsgType[Registered]
    registered.requestId mustBe 44
    f.router.underlyingActor.registrations must have size(1)
    f.router.underlyingActor.registrations(registered.registrationId) must have (
      'id (registered.registrationId),
      'callee (self),
      'procedure ("mypp.procedure")
    )
  }


  it should "confirm existing registration on incoming repeated REGISTER('same.procedure') from same callee" in { f =>
    f.router ! Hello(details = Dict().addRoles("callee"))
    expectMsgType[Welcome]
    f.router.underlyingActor.registrations mustBe empty
    
    f.router ! Register(44, procedure = "same.procedure")
    expectMsgType[Registered]
    f.router.underlyingActor.registrations must have size(1)
    
    f.router ! Register(55, procedure = "same.procedure")
    val registered = expectMsgType[Registered]
    registered.requestId mustBe 55
    f.router.underlyingActor.registrations must have size(1)
    f.router.underlyingActor.registrations(registered.registrationId) must have (
      'id (registered.registrationId),
      'callee (self),
      'procedure ("same.procedure")
    )
  }


  it should "create new registration2 on incoming REGISTER('myapp.procedure2') from same client" in { f =>
    f.router ! Hello(details = Dict().addRoles("callee"))
    expectMsgType[Welcome]
    f.router.underlyingActor.registrations mustBe empty
    
    f.router ! Register(44, procedure = "mypp.procedure1")
    val registrationId1 = expectMsgType[Registered].registrationId
    f.router.underlyingActor.registrations must have size(1)
    
    f.router ! Register(55, procedure = "mypp.procedure2")
    val registered2 = expectMsgType[Registered]
    registered2.requestId mustBe 55
    f.router.underlyingActor.registrations must have size (2)
    f.router.underlyingActor.registrations(registrationId1) must have(
      'id (registrationId1),
      'callee (self),
      'procedure ("mypp.procedure1")
    )
    f.router.underlyingActor.registrations(registered2.registrationId) must have(
      'id (registered2.registrationId),
      'callee (self),
      'procedure ("mypp.procedure2")
    )
  }


  it should "deny registration on incoming REGISTER('existing.procedure1') from different client2 and reply ERROR" in { f =>
    val client1 = TestProbe("client1")
    client1.send(f.router, Hello(details = Dict().addRoles("callee")))
    client1.expectMsgType[Welcome]
    f.router.underlyingActor.registrations mustBe empty

    client1.send(f.router, Register(44, procedure = "existing.procedure1"))
    client1.expectMsgType[Registered]
    f.router.underlyingActor.registrations must have size(1)
    
    val client2 = TestProbe("client2")
    client2.send(f.router, Hello(details = Dict().addRoles("callee")))
    client2.expectMsgType[Welcome]
    f.router.underlyingActor.registrations must have size(1)
    
    client2.send(f.router, Register(55, procedure = "existing.procedure1"))
    client2.expectMsg(Error(Register.tpe, 55, error = "wamp.error.procedure_already_exists"))
    f.router.underlyingActor.registrations must have size(1)
  }

  
  // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

  it should "disconnect on incoming UNREGISTER if peer didn't open session" in { f =>
    f.router ! Unregister(66, 99)
    expectMsg(Disconnect)
    f.router.underlyingActor.sessions mustBe empty
  }

  
  it should "remove existing registration on UNREGISTER from callee" in { f =>
    f.router ! Hello(details = Dict().addRoles("callee"))
    expectMsgType[Welcome]
    f.router.underlyingActor.registrations mustBe empty
    
    f.router ! Register(44, procedure = "mypp.procedure")
    val registrationId = expectMsgType[Registered].registrationId
    f.router.underlyingActor.registrations must have size(1)
    
    f.router ! Unregister(66, registrationId)
    expectMsg(Unregistered(66))
    f.router.underlyingActor.registrations  mustBe empty
  }

  
  it should "reply ERROR on incoming UNREGISTER(unknownRegistrationId)" in { f =>
    f.router ! Hello(details = Dict().addRoles("callee"))
    expectMsgType[Welcome]
    f.router.underlyingActor.registrations mustBe empty

    f.router ! Unregister(66, registrationId = 999)
    expectMsg(Error(Unregister.tpe, 66, error = "wamp.error.no_such_registration"))
  }


  it should "reply ERROR on incoming UNREGISTER callee1's registrations when callee2 is asking" in { f =>
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

  
  it should "disconnect on incoming CALL if peer didn't open session" in { f =>
    f.router ! Call(444, procedure = "myapp.procedure")
    expectMsg(Disconnect)
    f.router.underlyingActor.sessions  mustBe empty
    f.router.underlyingActor.invocations  mustBe empty
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


  it should "disconnect on incoming YIELD if peer didn't open session" in { f =>
    f.router ! Yield(1)
    expectMsg(Disconnect)
    f.router.underlyingActor.sessions mustBe empty
    f.router.underlyingActor.invocations mustBe empty
  }
  
  
  it should "send RESULT to caller on YIELD from callee" in { f =>
    val client1 = TestProbe("caller")
    val call = Call(444, procedure = "myapp.procedure")
    f.router.underlyingActor.invocations += (888L -> new Dealer.OutstandingInvocation(client1.ref, call))
    
    val client2 = TestProbe("callee")
    client2.send(f.router, Hello(details = Dict().addRoles("callee")))
    client2.expectMsgType[Welcome]
    
    val payload = Payload(List(44.23,"paolo",null,true))
    client2.send(f.router, Yield(888, payload = payload))
    client1.expectMsg(Result(444, payload = payload))
  }
}
