package akka.wamp.router

import akka.wamp._
import akka.wamp.messages._
import akka.wamp.serialization._

import scala.concurrent.duration._

/**
  * It tests the router running with its default settings 
  * (NO custom configuration is applied)
  */
class DefaultBrokerSpec extends RouterBaseSpec {


  // ~~~~~~~~~~~ ~~~~~~~~~~~ ~~~~~~~~~~~ ~~~~~~~~~~~
  //
  //  S U B S C R I B E   scenarios
  //
  // ~~~~~~~~~~~ ~~~~~~~~~~~ ~~~~~~~~~~~ ~~~~~~~~~~~


  // (1)
  "The default broker" should "disconnect on incoming SUBSCRIBE if client didn't open session" in { f =>
    f.router ! Subscribe(1, topic = "mypp.topic")
    expectMsg(Disconnect)
    f.router.underlyingActor.sessions mustBe empty
    f.router.underlyingActor.subscriptions mustBe empty
  }


  // (2)
  it should "create the first subscription upon receiving the first SUBSCRIBE" in { f =>
    val aClient = f.joinRealm("default")
    f.router.underlyingActor.subscriptions mustBe empty

    aClient.send(f.router, Subscribe(1, topic = "someTopic"))
    val subscribed = aClient.expectMsgType[Subscribed]
    subscribed.requestId must equal(1)

    f.router.underlyingActor.subscriptions must have size(1)
    f.router.underlyingActor.subscriptions(subscribed.subscriptionId) must have (
      'id (subscribed.subscriptionId),
      'topic ("someTopic"),
      'realm ("default"),
      'subscribers (Set(aClient.ref))
    )
    /*
     *   id | subscription(id, topic, realm, subscribers)
     *   ---+---------------------------------------------
     *    1 | (1, someTopic, default, [aClient])
     */
  }


  // (3)
  it should "confirm existing subscription upon receiving repeated SUBSCRIBE('sameTopic') from the same client/session joining the default realm" in { f =>
    val soleClient = f.joinRealm("default")
    f.router.underlyingActor.subscriptions mustBe empty

    soleClient.send(f.router, Subscribe(1, topic = "sameTopic"))
    val subscribed1 = soleClient.expectMsgType[Subscribed]
    f.router.underlyingActor.subscriptions must have size(1)

    soleClient.send(f.router, Subscribe(2, topic = "sameTopic"))
    val subscribed2 = soleClient.expectMsgType[Subscribed]
    subscribed2.requestId must equal(2)
    subscribed2.subscriptionId must equal(subscribed1.subscriptionId)

    f.router.underlyingActor.subscriptions must have size(1)
    f.router.underlyingActor.subscriptions(subscribed1.subscriptionId) must have (
      'id (subscribed2.subscriptionId),
      'topic ("sameTopic"),
      'realm ("default"),
      'subscribers (Set(soleClient.ref))
    )
    /*
     *   id | subscription(id, topic, realm, subscribers)
     *   ---+---------------------------------------------
     *    1 | (1, sameTopic, default, [soleClient])
     */
  }


  // (4)
  it should "create an additional subscription upon receiving SUBSCRIBE('sameTopic') from another client/session joining a different realm" in { f =>
    val aClient = f.joinRealm("default")
    f.router.underlyingActor.subscriptions mustBe empty

    aClient.send(f.router, Subscribe(1, topic = "sameTopic"))
    val subscribed1 = aClient.expectMsgType[Subscribed]
    f.router.underlyingActor.subscriptions must have size(1)

    val anotherClient = f.joinRealm("different")
    anotherClient.send(f.router, Subscribe(1, topic = "sameTopic"))
    val subscribed2 = anotherClient.expectMsgType[Subscribed]
    subscribed2.requestId must equal(1)
    subscribed2.subscriptionId mustNot equal(subscribed1.subscriptionId)

    f.router.underlyingActor.subscriptions must have size(2)
    f.router.underlyingActor.subscriptions(subscribed1.subscriptionId) must have (
      'id (subscribed1.subscriptionId),
      'topic ("sameTopic"),
      'realm ("default"),
      'subscribers (Set(aClient.ref))
    )
    f.router.underlyingActor.subscriptions(subscribed2.subscriptionId) must have (
      'id (subscribed2.subscriptionId),
      'topic ("sameTopic"),
      'realm ("different"),
      'subscribers (Set(anotherClient.ref))
    )
    /*
     *   id | subscription(id, topic, realm, subscribers)
     *   ---+---------------------------------------------
     *    1 | (1, sameTopic, default, [aClient])
     *    2 | (2, sameTopic, different, [anotherClient])
     */
  }


  // (5)
  it should "create an additional subscription upon receiving SUBSCRIBE('anotherTopic') from same client/session joining the default realm" in { f =>
    val sameClient = f.joinRealm("default")
    sameClient.send(f.router, Subscribe(1, topic = "someTopic"))
    val subscribed1 = sameClient.expectMsgType[Subscribed]

    sameClient.send(f.router, Subscribe(1, topic  ="anotherTopic"))
    val subscribed2 = sameClient.expectMsgType[Subscribed]
    subscribed2.requestId must equal(1)
    subscribed2.subscriptionId mustNot equal(subscribed1.subscriptionId)

    f.router.underlyingActor.subscriptions must have size(2)
    f.router.underlyingActor.subscriptions(subscribed1.subscriptionId) must have (
      'id (subscribed1.subscriptionId),
      'topic ("someTopic"),
      'realm ("default"),
      'subscribers (Set(sameClient.ref))
    )
    f.router.underlyingActor.subscriptions(subscribed2.subscriptionId) must have (
      'id (subscribed2.subscriptionId),
      'topic ("anotherTopic"),
      'realm ("default"),
      'subscribers (Set(sameClient.ref))
    )
    /*
     *   id | subscription(id, topic, realm, subscribers)
     *   ---+---------------------------------------------
     *    1 | (1, someTopic, default, [sameClient])
     *    2 | (2, anotherTopic, default, [sameClient])
     */

  }


  // (6)
  it should "update existing subscription upon receiving SUBSCRIBE('existingTopic') from another client/session joining the same realm" in { f =>
    val aClient = f.joinRealm("default")
    f.router.underlyingActor.subscriptions mustBe empty

    aClient.send(f.router, Subscribe(1, topic = "existingTopic"))
    val subscribed1 = aClient.expectMsgType[Subscribed]
    f.router.underlyingActor.subscriptions must have size(1)

    val anotherClient = f.joinRealm("default")
    anotherClient.send(f.router, Subscribe(1, topic = "existingTopic"))
    val subscribed2 = anotherClient.expectMsgType[Subscribed]
    subscribed2.requestId must equal(1)
    subscribed2.subscriptionId must equal(subscribed1.subscriptionId)

    f.router.underlyingActor.subscriptions must have size(1)
    f.router.underlyingActor.subscriptions(subscribed2.subscriptionId) must have (
      'id (subscribed2.subscriptionId),
      'topic ("existingTopic"),
      'realm ("default"),
      'subscribers (Set(aClient.ref, anotherClient.ref))
    )
    /*
     *   id | subscription(id, topic, realm, subscribers)
     *   ---+---------------------------------------------
     *    1 | (1, existingTopic, default, [aClient, anotherClient])
     */
  }





  // ~~~~~~~~~~~ ~~~~~~~~~~~ ~~~~~~~~~~~ ~~~~~~~~~~~
  //
  //  U N S U B S C R I B E   scenarios
  //
  // ~~~~~~~~~~~ ~~~~~~~~~~~ ~~~~~~~~~~~ ~~~~~~~~~~~

  // (7)
  it should "disconnect on incoming UNSUBSCRIBE if peer didn't open session" in { f =>
    f.router ! Unsubscribe(1, 1)
    expectMsg(Disconnect)
    f.router.underlyingActor.sessions mustBe empty
  }



  // (8)
  it should "reply ERROR upon receiving UNSUBSCRIBE for unknown subscription" in { f =>
    f.router.underlyingActor.subscriptions mustBe empty

    val client1 = f.joinRealm("default")
    client1.send(f.router, Unsubscribe(requestId = 1, /* unknown */ subscriptionId = 1))
    client1.expectMsg(Error(Unsubscribe.tpe, 1, Dict(), "wamp.error.no_such_subscription"))

    f.router.underlyingActor.subscriptions mustBe empty
  }



  // (9)
  it should "remove existing subscription upon receiving UNSUBSCRIBE from the sole subscriber" in { f =>
    val soleClient = f.joinRealm("default")
    f.router.underlyingActor.subscriptions mustBe empty

    soleClient.send(f.router, Subscribe(1, topic = "someTopic"))
    val subscriptionId = soleClient.expectMsgType[Subscribed].subscriptionId
    f.router.underlyingActor.subscriptions must have size(1)

    soleClient.send(f.router, Unsubscribe(requestId = 2, subscriptionId))
    soleClient.expectMsg(Unsubscribed(2))
    f.router.underlyingActor.subscriptions mustBe empty
  }


  // (10)
  it should "reply ERROR upon receiving UNSUBSCRIBE from another subscriber who did not apply for an existing subscription" in { f =>
    val aClient = f.joinRealm("default")
    f.router.underlyingActor.subscriptions mustBe empty

    aClient.send(f.router, Subscribe(requestId = 1, topic = "someTopic"))
    val subscriptionId = aClient.expectMsgType[Subscribed].subscriptionId
    f.router.underlyingActor.subscriptions must have size (1)

    val anotherClient = f.joinRealm("default")
    anotherClient.send(f.router, Unsubscribe(requestId = 1, subscriptionId))
    val error = anotherClient.expectMsgType[Error]
    error must have (
      'requestType (Unsubscribe.tpe),
      'requestId (1),
      'error ("wamp.error.no_such_subscription")
    )
    f.router.underlyingActor.subscriptions must have size (1)
  }



  // (11)
  it should "update existing subscription upon receiving UNSUBSCRIBE from legal subscriber" in { f =>
    val client1 = f.joinRealm("default")
    client1.send(f.router, Subscribe(1, topic = "someTopic"))
    val subscriptionId = client1.expectMsgType[Subscribed].subscriptionId

    val client2 = f.joinRealm("default")
    client2.send(f.router, Subscribe(1, topic = "someTopic"))
    client2.expectMsgType[Subscribed]

    client1.send(f.router, Unsubscribe(2, subscriptionId))
    client1.expectMsg(Unsubscribed(2))

    f.router.underlyingActor.subscriptions must have size(1)
    f.router.underlyingActor.subscriptions(subscriptionId) must have (
      'id (subscriptionId),
      'topic ("someTopic"),
      'realm ("default"),
      'subscribers (Set(client2.ref))
    )
  }


  // (12)
  it should "remove existing subscription upon receiving UNSUBSCRIBE from the last subscriber left" in { f =>
    val client1 = f.joinRealm("default")
    client1.send(f.router, Subscribe(1, topic = "someTopic"))
    val subscriptionId = client1.expectMsgType[Subscribed].subscriptionId

    val client2 = f.joinRealm("default")
    client2.send(f.router, Subscribe(1, topic = "someTopic"))
    client2.expectMsgType[Subscribed]

    client1.send(f.router, Unsubscribe(2, subscriptionId))
    client1.expectMsg(Unsubscribed(2))

    client2.send(f.router, Unsubscribe(2, subscriptionId))
    client2.expectMsg(Unsubscribed(2))

    // no more subscribers left
    f.router.underlyingActor.subscriptions  mustBe empty
  }





  // ~~~~~~~~~~~ ~~~~~~~~~~~ ~~~~~~~~~~~ ~~~~~~~~~~~
  //
  //   P U B L I S H   and   E V E N T   scenarios
  //
  // ~~~~~~~~~~~ ~~~~~~~~~~~ ~~~~~~~~~~~ ~~~~~~~~~~~


  // (13)
  it should "disconnect on incoming PUBLISH if client didn't open session" in { f =>
    f.router ! Publish(1, topic = "someTopic")
    expectMsg(Disconnect)
    f.router.underlyingActor.sessions mustBe empty
    f.router.underlyingActor.publications mustBe empty
  }


  // (14)
  it should "dispatch EVENT upon receiving PUBLISH for a topic subscribed in the same realm" in { f =>
    /*
     * Make three subscriber clients. Two of them join the default realm
     * while the other subscriber joins a different one
     */
    val client1 = f.joinRealm("default")
    val client2 = f.joinRealm("default")
    val client3 = f.joinRealm("another")

    f.router.underlyingActor.subscriptions mustBe empty
    f.router.underlyingActor.publications mustBe empty

    /*
     * Make all subscriber clients subscribe the same topic name,
     * although one of them belongs to a different realm
     */
    client1.send(f.router, Subscribe(1, topic = "sameTopic"))
    client1.expectMsgType[Subscribed]
    f.router.underlyingActor.subscriptions must have size(1)
    f.router.underlyingActor.publications mustBe empty

    client2.send(f.router, Subscribe(1, topic = "sameTopic"))
    client2.expectMsgType[Subscribed]
    f.router.underlyingActor.subscriptions must have size(1)
    f.router.underlyingActor.publications mustBe empty

    client3.send(f.router, Subscribe(1, topic = "sameTopic"))
    client3.expectMsgType[Subscribed]
    f.router.underlyingActor.subscriptions must have size(2)
    f.router.underlyingActor.publications mustBe empty

    /*
     *   id | subscription(id, topic, realm, subscribers)
     *   ---+---------------------------------------------
     *    1 | (1, sameTopic, default, [client1, client2])
     *    2 | (2, sameTopic, another, [client3])
     */

    /*
     * Make a publisher client. It joins the default realm and publishes
     * to the topic name subscribed as per above
     */
    val client4 = f.joinRealm("default")

    val payload = Payload(List(44.23, "paolo", null, true))
    client4.send(f.router, Publish(1, Dict("acknowledge" -> true), "sameTopic", payload))
    client4.expectMsg(Published(1, 5))
    f.router.underlyingActor.publications must have size(1)

    /*
     * Assert that the subscribers which joined the default realm receive the event
     * while the subscriber which joined a different realm doesn't
     */
    client1.expectMsg(Event(1, 5, Dict(), payload))
    client2.expectMsg(Event(1, 5, Dict(), payload))
    client3.expectNoMsg(0.second)
  }
}
