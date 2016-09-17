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
class BrokerSpec extends RouterFixtureSpec {

  "The default router as broker" should "fail connection on SUBSCRIBE but no session open" in { f =>
    // TODO https://github.com/angiolep/akka-wamp/issues/22
    pending
    f.router ! Subscribe(1, topic = "mypp.topic")
    expectMsg(???)
  }
  

  it should "reply ERROR on SUBSCRIBE but no 'subscriber' role in session" in { f =>
    val client = TestProbe("client")
    client.send(f.router , Hello(details = Dict().addRoles( Roles.publisher)))
    client.receiveOne(0.seconds)
    client.send(f.router, Subscribe(1, topic = "mypp.topic1"))
    client.expectMsg(Error(Subscribe.tpe, 1, Error.defaultDetails, "akka.wamp.error.no_subscriber_role"))
    f.router.underlyingActor.subscriptions mustBe empty
    client.expectNoMsg()
  }


  it should "create new subscription on first SUBSCRIBE" in { f =>
    val client = TestProbe("client")
    client.send(f.router , Hello()); client.receiveOne(0.seconds)
    client.send(f.router, Subscribe(1, topic = "mypp.topic"))
    client.receiveOne(0.seconds) match {
      case Subscribed(requestId, subscriptionId) =>
        requestId mustBe 1
        f.router.underlyingActor.subscriptions must have size(1)
        f.router.underlyingActor.subscriptions(subscriptionId) must have (
          'id (subscriptionId),
          'subscribers (Set(client.ref)),
          'topic ("mypp.topic")
        )
      case m => fail(s"unexpected $m")
    }
  }


  it should "confirm existing subscription on repeated SUBSCRIBE to same topic" in { f =>
    val client = TestProbe("client")
    client.send(f.router , Hello()); client.receiveOne(0.seconds)
    client.send(f.router, Subscribe(1, topic = "mypp.topic")); client.receiveOne(0.seconds)
    client.send(f.router, Subscribe(2, topic = "mypp.topic"))
    client.receiveOne(0.seconds) match {
      case Subscribed(requestId, subscriptionId) =>
        requestId mustBe 2
        f.router.underlyingActor.subscriptions must have size(1)
        f.router.underlyingActor.subscriptions(subscriptionId) must have (
          'id (subscriptionId),
          'subscribers (Set(client.ref)),
          'topic ("mypp.topic")
        )
      case m => fail(s"unexpected $m")
    }
  }


  it should "create new subscription2 on SUBSCRIBE to topic2 from same client" in { f =>
    val client = TestProbe("client")
    client.send(f.router , Hello()); client.receiveOne(0.seconds)
    client.send(f.router, Subscribe(1, topic = "mypp.topic1"))
    val id1 = client.receiveOne(0.seconds).asInstanceOf[Subscribed].subscriptionId
    client.send(f.router, Subscribe(2, topic  ="mypp.topic2"))
    client.receiveOne(0.seconds) match {
      case Subscribed(_, id2) =>
        f.router.underlyingActor.subscriptions must have size(2)
        f.router.underlyingActor.subscriptions(id1) must have (
          'id (id1),
          'subscribers (Set(client.ref)),
          'topic ("mypp.topic1")
        )
        f.router.underlyingActor.subscriptions(id2) must have (
          'id (id2),
          'subscribers (Set(client.ref)),
          'topic ("mypp.topic2")
        )
      case m => fail(s"unexpected $m")
    }
  }


  it should "update existing subscription1 on SUBSCRIBE to topic1 from different client2" in { f =>
    val client1 = TestProbe("client1")
    client1.send(f.router , Hello()); client1.receiveOne(0.seconds)
    client1.send(f.router, Subscribe(1, topic = "mypp.topic")); client1.receiveOne(0.second)
    
    val client2 = TestProbe("client2")
    client2.send(f.router , Hello()); client2.receiveOne(0.seconds)
    client2.send(f.router, Subscribe(2, topic = "mypp.topic"))
    client2.receiveOne(0.seconds) match {
      case Subscribed(requestId, subscriptionId) =>
        requestId mustBe 2
        f.router.underlyingActor.subscriptions must have size(1)
        f.router.underlyingActor.subscriptions(subscriptionId) must have (
          'id (subscriptionId),
          'subscribers (Set(client1.ref, client2.ref)),
          'topic ("mypp.topic")
        )
      case m => fail(s"unexpected $m")
    }
  }


  it should "update existing subscription1 on UNSUBSCRIBE topic1 from some client2" in { f =>
    val client1 = TestProbe("client1")
    client1.send(f.router , Hello()); client1.receiveOne(0.seconds)
    client1.send(f.router, Subscribe(1, topic = "mypp.topic1"))
    val sid11 = client1.receiveOne(0.second).asInstanceOf[Subscribed].subscriptionId
    
    val client2 = TestProbe("client2")
    client2.send(f.router , Hello()); client2.receiveOne(0.seconds)
    client2.send(f.router, Subscribe(2, topic = "mypp.topic1"))
    val sid12 = client2.receiveOne(0.seconds).asInstanceOf[Subscribed].subscriptionId
    sid11 must equal(sid12)
    client2.send(f.router, Unsubscribe(3, sid12))
    client2.expectMsg(Unsubscribed(3))
    f.router.underlyingActor.subscriptions must have size(1)
    f.router.underlyingActor.subscriptions(sid11) must have (
      'id (sid11),
      'subscribers (Set(client1.ref)),
      'topic ("mypp.topic1")
    )
  }


  it should "remove existing subscription1 on UNSUBSCRIBE topic1 from the sole client1 left" in { f =>
    val client = TestProbe("client1")
    client.send(f.router , Hello()); client.receiveOne(0.seconds)
    client.send(f.router, Subscribe(1, topic = "mypp.topic"))
    val sid = client.receiveOne(0.seconds).asInstanceOf[Subscribed].subscriptionId
    client.send(f.router, Unsubscribe(2, sid))
    client.expectMsg(Unsubscribed(2))
    f.router.underlyingActor.subscriptions  mustBe empty
  }


  it should "reply ERROR on UNSUBSCRIBE unknown subscription" in { f =>
    val client = TestProbe("client1")
    client.send(f.router , Hello()); client.receiveOne(0.seconds)
    client.send(f.router, Unsubscribe(1, 9999))
    client.expectMsg(Error(Unsubscribe.tpe, 1, Error.defaultDetails, "wamp.error.no_such_subscription"))
  }
  
  
  // ~~~~~~~~~~~~~~~~~~~

  
  it should "fail connection on PUBLISH but no session open" in { f =>
    // TODO https://github.com/angiolep/akka-wamp/issues/22
    pending
    f.router ! Publish(1, topic = "mypp.topic1")
    expectMsg(???)
  }

  
  it should "reply ERROR on PUBLISH(ack) but no 'publisher' role in session" in { f =>
    val client = TestProbe("client")
    client.send(f.router , Hello(details = Dict().addRoles(Roles.callee)))
    client.receiveOne(0.seconds)
    client.send(f.router, Publish(1, options = Dict("acknowledge" -> true), "mypp.topic1"))
    client.expectMsg(Error(Publish.tpe, 1, Error.defaultDetails, "akka.wamp.error.no_publisher_role"))
    client.expectNoMsg()
    f.router.underlyingActor.publications mustBe empty
  }

  
  it should "do nothing on PUBLISH(noack) but no 'publisher' role in session" in { f =>
    val client = TestProbe("client")
    client.send(f.router , Hello(details = Dict().addRoles(Roles.callee)))
    client.receiveOne(0.seconds)
    client.send(f.router, Publish(1, topic = "mypp.topic1"))
    client.expectNoMsg()
    f.router.underlyingActor.publications mustBe empty
  }


  it should "dispatch EVENT on PUBLISH to a topic with subscribers" in { f =>
    val client1 = TestProbe("client1")
    client1.send(f.router , Hello(details = Dict().addRoles(Roles.subscriber)))
    client1.receiveOne(0.seconds)
    val client2 = TestProbe("client2")
    client2.send(f.router , Hello(details = Dict().addRoles(Roles.subscriber, Roles.publisher)))
    client2.receiveOne(0.seconds)
    val client3 = TestProbe("client3")
    client3.send(f.router , Hello(details = Dict().addRoles( Roles.publisher)))
    client3.receiveOne(0.seconds)
    
    client1.send(f.router, Subscribe(1, topic = "mypp.topic1")); client1.receiveOne(1.second)
    client2.send(f.router, Subscribe(1, topic = "mypp.topic1"));client2.receiveOne(0.seconds)
    
    val payload = Payload(List(44.23,"paolo",null,true))
    client3.send(f.router, Publish(1, Dict("acknowledge" -> true), "mypp.topic1", Some(payload)))
    
    client1.expectMsg(Event(1, 4, Dict(), Some(payload)))
    client2.expectMsg(Event(1, 4, Dict(), Some(payload)))
    client3.expectMsg(Published(1, 4))
    client3.expectNoMsg()

    f.router.underlyingActor.publications must have size(1)
  }

  
}
