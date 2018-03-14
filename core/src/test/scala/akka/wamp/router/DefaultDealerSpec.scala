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
class DefaultDealerSpec extends RouterBaseSpec {

  // ~~~~~~~~~~~ ~~~~~~~~~~~ ~~~~~~~~~~~ ~~~~~~~~~~~
  //
  //  R E G I S T E R   scenarios
  //
  // ~~~~~~~~~~~ ~~~~~~~~~~~ ~~~~~~~~~~~ ~~~~~~~~~~~


  // (1)
  "The default dealer" should "disconnect on incoming REGISTER if peer didn't open session" in { f =>
    f.router ! Register(1, procedure = "myapp.procedure")
    expectMsg(Disconnect)
    f.router.underlyingActor.sessions  mustBe empty
    f.router.underlyingActor.registrations  mustBe empty
  }



  // (2)
  it should "create the first registration upon receiving the first REGISTER" in { f =>
    val aClient = f.joinRealm("default")
    f.router.underlyingActor.registrations mustBe empty

    aClient.send(f.router, Register(1, procedure = "someProcedure"))
    val registered = aClient.expectMsgType[Registered]
    registered.requestId mustBe 1

    f.router.underlyingActor.registrations must have size(1)
    f.router.underlyingActor.registrations(registered.registrationId) must have (
      'id (registered.registrationId),
      'procedure ("someProcedure"),
      'realm ("default"),
      'callee (aClient.ref)
    )
    /*
     *   id | registrations(id, procedure, realm, callee)
     *   ---+---------------------------------------------
     *    1 | (1, someProcedure, default, aClient)
     */
  }



  // (3)
  it should "confirm existing registration upon receiving repeated REGISTER('someProcedure') from same callee" in { f =>
    val soleClient = f.joinRealm("default")
    f.router.underlyingActor.registrations mustBe empty

    soleClient.send(f.router, Register(1, procedure = "someProcedure"))
    val registered1 = soleClient.expectMsgType[Registered]
    f.router.underlyingActor.registrations must have size(1)

    soleClient.send(f.router, Register(2, procedure = "someProcedure"))
    val registered2 = soleClient.expectMsgType[Registered]
    registered2.requestId mustBe 2
    registered2.registrationId must equal(registered1.registrationId)

    f.router.underlyingActor.registrations must have size(1)
    f.router.underlyingActor.registrations(registered1.registrationId) must have (
      'id (registered2.registrationId),
      'procedure ("someProcedure"),
      'realm ("default"),
      'callee (soleClient.ref)
    )
    /*
     *   id | registrations(id, procedure, realm, callee)
     *   ---+---------------------------------------------
     *    1 | (1, someProcedure, default, soleClient)
     */
  }



  // (4)
  it should "create an additional registration upon receiving REGISTER('existingProcedure') from client/session joining a different realm" in { f =>
    val aClient = f.joinRealm("default")
    f.router.underlyingActor.registrations mustBe empty

    aClient.send(f.router, Register(1, procedure = "existingProcedure"))
    val registered1 = aClient.expectMsgType[Registered]
    f.router.underlyingActor.registrations must have size(1)

    val anotherClient = f.joinRealm("different")
    anotherClient.send(f.router, Register(1, procedure = "existingProcedure"))
    val registered2 = anotherClient.expectMsgType[Registered]
    registered2.requestId mustBe 1

    f.router.underlyingActor.registrations must have size (2)
    f.router.underlyingActor.registrations(registered1.registrationId) must have(
      'id (registered1.registrationId),
      'procedure ("existingProcedure"),
      'realm ("default"),
      'callee (aClient.ref)
    )
    f.router.underlyingActor.registrations(registered2.registrationId) must have(
      'id (registered2.registrationId),
      'procedure ("existingProcedure"),
      'realm ("different"),
      'callee (anotherClient.ref)
    )
    /*
     *   id | registration(id, procedure, realm, callee)
     *   ---+---------------------------------------------
     *    1 | (1, existingProcedure, default, aClient)
     *    2 | (2, existingProcedure, different, anotherClient)
     */
  }


  // (5)
  it should "create an additional registration upon receiving REGISTER('anotherProcedure') from same client/session joining the default realm" in { f =>
    val sameClient = f.joinRealm("default")
    sameClient.send(f.router, Register(1, procedure = "someProcedure"))
    val registered1 = sameClient.expectMsgType[Registered]

    sameClient.send(f.router, Register(1, procedure = "anotherProcedure"))
    val registered2 = sameClient.expectMsgType[Registered]
    registered2.requestId must equal(1)
    registered2.registrationId mustNot equal(registered1.registrationId)

    f.router.underlyingActor.registrations must have size(2)
    f.router.underlyingActor.registrations(registered1.registrationId) must have (
      'id (registered1.registrationId),
      'procedure ("someProcedure"),
      'realm ("default"),
      'callee (sameClient.ref)
    )
    f.router.underlyingActor.registrations(registered2.registrationId) must have (
      'id (registered2.registrationId),
      'procedure ("anotherProcedure"),
      'realm ("default"),
      'callee (sameClient.ref)
    )
    /*
     *   id | registration(id, procedure, realm, callee)
     *   ---+---------------------------------------------
     *    1 | (1, someProcedure, default, sameClient)
     *    2 | (2, anotherProcedure, default, sameClient)
     */

  }



  //(6)
  it should "reply ERROR upon receiving REGISTER('existingProcedure') from different client joining the same default realm" in { f =>
    val client1 = f.joinRealm("default")
    f.router.underlyingActor.registrations mustBe empty

    client1.send(f.router, Register(1, procedure = "existingProcedure"))
    client1.expectMsgType[Registered]
    f.router.underlyingActor.registrations must have size(1)
    
    val client2 = f.joinRealm("default")
    client2.send(f.router, Register(1, procedure = "existingProcedure"))
    client2.expectMsg(Error(Register.tpe, 1, error = "wamp.error.procedure_already_exists"))
    f.router.underlyingActor.registrations must have size(1)
  }




  // ~~~~~~~~~~~ ~~~~~~~~~~~ ~~~~~~~~~~~ ~~~~~~~~~~~
  //
  //  U N R E G I S T E R    scenarios
  //
  // ~~~~~~~~~~~ ~~~~~~~~~~~ ~~~~~~~~~~~ ~~~~~~~~~~~


  // (7)
  it should "disconnect on incoming UNREGISTER if peer didn't open session" in { f =>
    f.router ! Unregister(66, 99)
    expectMsg(Disconnect)
    f.router.underlyingActor.sessions mustBe empty
  }


  // (8)
  it should "reply ERROR upon receiving UNREGISTER for unknown registration" in { f =>
    val client = f.joinRealm("default")
    f.router.underlyingActor.registrations mustBe empty

    client.send(f.router, Unregister(1, registrationId = 1))
    client.expectMsg(Error(Unregister.tpe, 1, error = "wamp.error.no_such_registration"))
  }


  // (9)
  it should "remove existing registration upon receiving UNREGISTER from the sole callee" in { f =>
    val soleClient = f.joinRealm("default")
    f.router.underlyingActor.registrations mustBe empty

    soleClient.send(f.router, Register(1, procedure = "someProcedure"))
    val registrationId = soleClient.expectMsgType[Registered].registrationId
    f.router.underlyingActor.registrations must have size(1)

    soleClient.send(f.router, Unregister(2, registrationId))
    soleClient.expectMsg(Unregistered(2))
    f.router.underlyingActor.registrations mustBe empty
  }

  

  // (10)
  it should "reply ERROR upon receiving UNREGISTER from another callee who did not apply for an existing registration" in { f =>
    val client1 = f.joinRealm("default")
    f.router.underlyingActor.registrations mustBe empty

    client1.send(f.router, Register(requestId = 1, procedure = "someProcedure"))
    val registrationId = client1.expectMsgType[Registered].registrationId
    f.router.underlyingActor.registrations must have size(1)

    val client2 = f.joinRealm("default")
    f.router.underlyingActor.registrations must have size(1)

    client2.send(f.router, Unregister(requestId = 1, registrationId))
    client2.expectMsg(Error(Unregister.tpe, 1, error = "wamp.error.no_such_registration"))
  }


  // (11)
  // noop

  // (12)
  // noop


  // ~~~~~~~~~~~ ~~~~~~~~~~~ ~~~~~~~~~~~ ~~~~~~~~~~~
  //
  //   C A L L   and   I N V O C A T I O N  scenarios
  //
  // ~~~~~~~~~~~ ~~~~~~~~~~~ ~~~~~~~~~~~ ~~~~~~~~~~~


  // (13)
  it should "disconnect on incoming CALL if peer didn't open session" in { f =>
    f.router ! Call(444, procedure = "myapp.procedure")
    expectMsg(Disconnect)
    f.router.underlyingActor.sessions  mustBe empty
    f.router.underlyingActor.invocations  mustBe empty
  }


  // (14)
  it should "dispatch INVOCATION on CALL for a procedure registered in the same realm" in { f =>
    /*
     * Make three callee clients. Two of them join the same default realm
     * while the other subscriber joins a different one
     */
    val client1 = f.joinRealm("default")
    val client2 = f.joinRealm("default")
    val client3 = f.joinRealm("another")

    f.router.underlyingActor.registrations mustBe empty
    f.router.underlyingActor.invocations mustBe empty

    /*
     * Make all the callee clients register the same procedure name,
     * although one will belong to a different realm
     */
    client1.send(f.router, Register(1, procedure = "myapp.procedure"))
    val registrationId1 = client1.expectMsgType[Registered].registrationId

    client2.send(f.router, Register(1, procedure = "myapp.procedure"))
    client2.expectMsgType[Error]

    client3.send(f.router, Register(1, procedure = "myapp.procedure"))
    val registrationId3 = client3.expectMsgType[Registered].registrationId

    f.router.underlyingActor.registrations must have size(2)
    f.router.underlyingActor.invocations mustBe empty

    /*
     * Make a caller client. It joins the default realm and calls
     * the procedure name registered as per above
     */
    val client4 = f.joinRealm("default")

    client4.send(f.router, Call(1, procedure = "myapp.procedure"))
    client4.expectNoMessage(0.seconds)

    f.router.underlyingActor.registrations must have size(2)
    f.router.underlyingActor.invocations must have size(1)

    /*
     * Assert that the callee which joined the default realm receive the invocation
     * while the callee which joined a different realm doesn't
     */
    client1.expectMsg(Invocation(1, registrationId1, Invocation.defaultDetails))
    client2.expectNoMessage(0.seconds)
    client3.expectNoMessage(0.seconds)
  }




  // ~~~~~~~~~~~ ~~~~~~~~~~~ ~~~~~~~~~~~ ~~~~~~~~~~~
  //
  //   Y I E L D   and   R E S U L T  scenarios
  //
  // ~~~~~~~~~~~ ~~~~~~~~~~~ ~~~~~~~~~~~ ~~~~~~~~~~~


  it should "disconnect on incoming YIELD if peer didn't open session" in { f =>
    f.router ! Yield(1)
    expectMsg(Disconnect)
    f.router.underlyingActor.sessions mustBe empty
    f.router.underlyingActor.invocations mustBe empty
  }
  
  
  it should "send RESULT to caller on YIELD from callee" in { f =>
    val client1 = f.joinRealm("default")
    f.router.underlyingActor.invocations += (888L -> new Dealer.OutstandingInvocation(client1.ref, Call(444L, procedure = "myapp.procedure")))

    val client2 = f.joinRealm("default")
    val payload = Payload(List(44.23,"paolo",null,true))
    client2.send(f.router, Yield(888L, payload = payload))
    client1.expectMsg(Result(444L, payload = payload))
  }
}
