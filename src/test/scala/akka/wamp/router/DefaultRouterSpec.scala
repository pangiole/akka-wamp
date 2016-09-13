package akka.wamp.router

import akka.testkit._
import akka.wamp._
import akka.wamp.messages._
import akka.wamp.serialization._

import scala.concurrent.duration._

/**
  * It tests the Router running with its default settings 
  * (when NO custom configuration is applied)
  */
class DefaultRouterSpec extends RouterFixtureSpec {

  "The default router actor"  should "auto-create realm if client says HELLO for unknown realm" in { fixture =>
    fixture.router ! Hello("myapp.realm", Dict().withRoles("publisher"))
    expectMsgType[Welcome]
    fixture.router.underlyingActor.realms must have size(2)
    fixture.router.underlyingActor.realms must contain allOf ("akka.wamp.realm", "myapp.realm")
  }
  
  it should "reply WELCOME if client says HELLO for existing realm" in { fixture =>
    fixture.router ! Hello("akka.wamp.realm", Dict().withRoles("publisher"))
    expectMsg(Welcome(1, Dict().withRoles("broker", "dealer").withAgent("akka-wamp-0.7.0")))
    fixture.router.underlyingActor.realms must have size(1)
    fixture.router.underlyingActor.realms must contain only ("akka.wamp.realm")
    fixture.router.underlyingActor.sessions must have size(1)
    val session = fixture.router.underlyingActor.sessions(1)
    session must have (
      'id (1),
      'client (testActor),
      'roles (Set("publisher")),
      'realm ("akka.wamp.realm")
    )
  }


  it should "fail if client says HELLO twice (regardless the realm)" in { fixture =>
    pending
    fixture.router ! Hello("akka.wamp.realm", Dict().withRoles("publisher"))
    receiveOne(0.seconds)
    fixture.router ! Hello("whatever.realm", Dict().withRoles("publisher"))
    expectMsg(???)
    fixture.router.underlyingActor.sessions  mustBe empty
  }


  it should "fail if client says GOODBYE before HELLO" in { fixture =>
    pending
    fixture.router ! Goodbye()
    expectMsg(???)
  }


  it should "reply GOODBYE if client says GOODBYE after HELLO" in { fixture =>
    fixture.router ! Hello("akka.wamp.realm", Dict().withRoles("publisher"))
    expectMsgType[Welcome]
    fixture.router ! Goodbye(Dict("message" -> "The host is shutting down now."), "wamp.error.system_shutdown")
    expectMsg(Goodbye(Goodbye.defaultDetails, "wamp.error.goodbye_and_out"))
    fixture.router.underlyingActor.sessions  mustBe empty
  }
  

  it should "fail if client says PUBLISH before session has opened" in { fixture =>
    pending
    fixture.router ! Publish(1, options = Dict(), "topic1")
    expectMsg(???)
    fixture.router.underlyingActor.publications mustBe empty
    
  }

  
  it should "reply ERROR if client says PUBLISH(ack) but has no publisher role" in { fixture =>
    val client = TestProbe("client")
    client.send(fixture.router , Hello("akka.wamp.realm", Dict().withRoles("subscriber")))
    client.receiveOne(0.seconds)
    client.send(fixture.router, Publish(1, options = Dict("acknowledge" -> true), "topic1"))
    client.expectMsg(Error(Publish.tpe, 1, Error.defaultDetails, "akka.wamp.error.no_publisher_role"))
    client.expectNoMsg()
    fixture.router.underlyingActor.publications mustBe empty
  }

  it should "stay silent if client says PUBLISH(noack) but has no publisher role" in { fixture =>
    val client = TestProbe("client")
    client.send(fixture.router , Hello("akka.wamp.realm", Dict().withRoles("subscriber")))
    client.receiveOne(0.seconds)
    client.send(fixture.router, Publish(1, options = Dict(), "topic1"))
    client.expectNoMsg()
    fixture.router.underlyingActor.publications mustBe empty
  }


  it should "dispatch EVENT if client says PUBLISH to a topic with subscribers" in { fixture =>
    val client1 = TestProbe("client1")
    client1.send(fixture.router , Hello("akka.wamp.realm", Dict().withRoles("subscriber")))
    client1.receiveOne(0.seconds)
    val client2 = TestProbe("client2")
    client2.send(fixture.router , Hello("akka.wamp.realm", Dict().withRoles("subscriber","publisher")))
    client2.receiveOne(0.seconds)
    val client3 = TestProbe("client3")
    client3.send(fixture.router , Hello("akka.wamp.realm", Dict().withRoles("publisher")))
    client3.receiveOne(0.seconds)
    
    client1.send(fixture.router, Subscribe(1, Dict(), "topic1")); client1.receiveOne(1.second)
    client2.send(fixture.router, Subscribe(1, Dict(), "topic1"));client2.receiveOne(0.seconds)
    
    val payload = Payload(List(44.23,"paolo",null,true))
    client3.send(fixture.router, Publish(1, Dict("acknowledge" -> true), "topic1", Some(payload)))
    
    client1.expectMsg(Event(1, 4, Dict(), Some(payload)))
    client2.expectMsg(Event(1, 4, Dict(), Some(payload)))
    client3.expectMsg(Published(1, 4))
    client3.expectNoMsg()

    fixture.router.underlyingActor.publications must have size(1)
  }

  
  it should  "fail if client says SUBSCRIBE before session has opened" in { fixture =>
    pending
    fixture.router ! Subscribe(1, Dict(), "topic1")
    expectMsg(???)
  }
  

  it should "reply ERROR if client has no subscriber role" in { fixture =>
    val client = TestProbe("client")
    client.send(fixture.router , Hello("akka.wamp.realm", Dict().withRoles("publisher")))
    client.receiveOne(0.seconds)
    client.send(fixture.router, Subscribe(1, Dict(), "topic1"))
    client.expectMsg(Error(Subscribe.tpe, 1, Error.defaultDetails, "akka.wamp.error.no_subscriber_role"))
    fixture.router.underlyingActor.subscriptions mustBe empty
    client.expectNoMsg()
  }

  
  it should "create a new subscription1 if client1 says SUBSCRIBE to topic1" in { fixture =>
    val client = TestProbe("client")
    client.send(fixture.router , Hello("akka.wamp.realm", Dict().withRoles("subscriber")))
    client.receiveOne(0.seconds)
    client.send(fixture.router, Subscribe(1, Dict(), "topic1"))
    client.receiveOne(0.seconds) match {
      case Subscribed(requestId, subscriptionId) =>
        requestId mustBe 1
        fixture.router.underlyingActor.subscriptions must have size(1)
        fixture.router.underlyingActor.subscriptions(subscriptionId) must have (
          'id (subscriptionId),
          'subscribers (Set(client.ref)),
          'topic ("topic1")
        )
      case m => fail(s"unexpected $m")
    } 
  }

  
  it should "confirm existing subscription1 any time client1 repeats SUBSCRIBE to topic1" in { fixture =>
    val client = TestProbe("client")
    client.send(fixture.router , Hello("akka.wamp.realm", Dict().withRoles("subscriber")))
    client.receiveOne(0.seconds)
    client.send(fixture.router, Subscribe(1, Dict(), "topic1"))
    client.receiveOne(0.seconds)
    client.send(fixture.router, Subscribe(2, Dict(), "topic1"))
    client.receiveOne(0.seconds) match {
      case Subscribed(requestId, subscriptionId) =>
        requestId mustBe 2
        fixture.router.underlyingActor.subscriptions must have size(1)
        fixture.router.underlyingActor.subscriptions(subscriptionId) must have (
          'id (subscriptionId),
          'subscribers (Set(client.ref)),
          'topic ("topic1")
        )
      case m => fail(s"unexpected $m")
    }
  }

  
  it should "create a new subscription2 if client1 says SUBSCRIBE to topic2" in { fixture =>
    val client = TestProbe("client")
    client.send(fixture.router , Hello("akka.wamp.realm", Dict().withRoles("subscriber")))
    client.receiveOne(0.seconds)
    client.send(fixture.router, Subscribe(1, Dict(), "topic1"))
    val id1 = client.receiveOne(0.seconds).asInstanceOf[Subscribed].subscriptionId
    client.send(fixture.router, Subscribe(2, Dict(), "topic2"))
    client.receiveOne(0.seconds) match {
      case Subscribed(_, id2) =>
        fixture.router.underlyingActor.subscriptions must have size(2)
        fixture.router.underlyingActor.subscriptions(id1) must have (
          'id (id1),
          'subscribers (Set(client.ref)),
          'topic ("topic1")
        )
        fixture.router.underlyingActor.subscriptions(id2) must have (
          'id (id2),
          'subscribers (Set(client.ref)),
          'topic ("topic2")
        )
      case m => fail(s"unexpected $m")
    }
  }

  
  it should "update existing subscription1 if also client2 says SUBSCRIBE to topic1" in { fixture =>
    val client1 = TestProbe("client1")
    client1.send(fixture.router , Hello("akka.wamp.realm", Dict().withRoles("subscriber")))
    client1.receiveOne(0.seconds)
    client1.send(fixture.router, Subscribe(1, Dict(), "topic1"))
    client1.receiveOne(0.second)
    val client2 = TestProbe("client2")
    client2.send(fixture.router , Hello("akka.wamp.realm", Dict().withRoles("subscriber","publisher")))
    client2.receiveOne(0.seconds)
    client2.send(fixture.router, Subscribe(2, Dict(), "topic1"))
    client2.receiveOne(0.seconds) match {
      case Subscribed(requestId, subscriptionId) =>
        requestId mustBe 2
        fixture.router.underlyingActor.subscriptions must have size(1)
        fixture.router.underlyingActor.subscriptions(subscriptionId) must have (
          'id (subscriptionId),
          'subscribers (Set(client1.ref, client2.ref)),
          'topic ("topic1")
        )
      case m => fail(s"unexpected $m")
    }
  }

  
  it should "update existing multiple-subscribers subscription1 if client2 says UNSUBSCRIBE" in { fixture =>
    val client1 = TestProbe("client1")
    client1.send(fixture.router , Hello("akka.wamp.realm", Dict().withRoles("subscriber")))
    client1.receiveOne(0.seconds)
    client1.send(fixture.router, Subscribe(1, Dict(), "topic1"))
    val sid11 = client1.receiveOne(0.second).asInstanceOf[Subscribed].subscriptionId
    val client2 = TestProbe("client2")
    client2.send(fixture.router , Hello("akka.wamp.realm", Dict().withRoles("subscriber","publisher")))
    client2.receiveOne(0.seconds)
    client2.send(fixture.router, Subscribe(2, Dict(), "topic1"))
    val sid12 = client2.receiveOne(0.seconds).asInstanceOf[Subscribed].subscriptionId
    sid11 must equal(sid12)
    client2.send(fixture.router, Unsubscribe(3, sid12))
    client2.expectMsg(Unsubscribed(3))
    fixture.router.underlyingActor.subscriptions must have size(1)
    fixture.router.underlyingActor.subscriptions(sid11) must have (
      'id (sid11),
      'subscribers (Set(client1.ref)),
      'topic ("topic1")
    )
  }

  
  it should "remove existing single-subscriber subscription2 if client1 says UNSUBSCRIBE" in { fixture =>
    val client = TestProbe("client1")
    client.send(fixture.router , Hello("akka.wamp.realm", Dict().withRoles("subscriber")))
    client.receiveOne(0.seconds)
    client.send(fixture.router, Subscribe(1, Dict(), "topic"))
    val sid = client.receiveOne(0.seconds).asInstanceOf[Subscribed].subscriptionId
    client.send(fixture.router, Unsubscribe(2, sid))
    client.expectMsg(Unsubscribed(2))
    fixture.router.underlyingActor.subscriptions  mustBe empty  
  }

  
  it should "reply ERROR if client says UNSUBSCRIBE from unknown subscription" in { fixture =>
    val client = TestProbe("client1")
    client.send(fixture.router , Hello("akka.wamp.realm", Dict().withRoles("subscriber")))
    client.receiveOne(0.seconds)
    client.send(fixture.router, Unsubscribe(1, 9999))
    client.expectMsg(Error(Unsubscribe.tpe, 1, Error.defaultDetails, "wamp.error.no_such_subscription"))
  }
}
