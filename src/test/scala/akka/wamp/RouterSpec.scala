package akka.wamp

import akka.actor.ActorSystem
import akka.testkit._
import akka.wamp.Messages._
import akka.wamp.Router._
import org.scalatest._

import scala.concurrent.duration._

class RouterSpec extends TestKit(ActorSystem()) with ImplicitSender with WordSpecLike with MustMatchers {
  "The router" when {
    "handling sessions" should {

      "reply ABORT if client says HELLO for unknown realm" in new RouterFixture {
        routerRef ! Hello("unknown.realm", dictWithRoles("whatever.role"))
        expectMsg(Abort(DictBuilder().withEntry("message", "The realm unknown.realm does not exist.").build(), "wamp.error.no_such_realm"))
        router.realms must have size(1)
        router.realms must contain only ("akka.wamp.realm")
        router.sessions mustBe empty
      }

      "auto-create realm if client says HELLO for unknown realm" in {
        pending
      }

      
      "reply WELCOME if client says HELLO for existing realm" in new RouterFixture {
        routerRef ! Hello("akka.wamp.realm", dictWithRoles("publisher"))
        expectMsg(Welcome(0, DictBuilder().withRoles(Set("broker")).withEntry("agent", "akka-wamp-0.1.0").build()))
        router.realms must have size(1)
        router.realms must contain only ("akka.wamp.realm")
        router.sessions must have size(1)
        val session = router.sessions(0)
        session must have (
          'id (0),
          'router (routerRef),
          'routerRoles (Set("broker")),
          'client (testActor),
          'clientRoles (Set("publisher")),
          'realm ("akka.wamp.realm")
        )
      }

      
      "fail if client says HELLO twice (regardless the realm)" in new RouterFixture {
        routerRef ! Hello("akka.wamp.realm", dictWithRoles("publisher"))
        receiveOne(0.seconds)
        routerRef ! Hello("whatever.realm", dictWithRoles("whatever.role"))
        expectMsg(Failure("Session was already open."))
        router.sessions must have size(0)
      }

      // TODO WAMP specs don't clarify if client can open a second connection attached to a different realm?
      
      
      "fail if client says GOODBYE before HELLO" in new RouterFixture {
        routerRef ! Goodbye(DictBuilder().build(), "whatever.reason")
        expectMsg(Failure("Session was not open yet."))
      }
      
      
      "reply GOODBYE if client says GOODBYE after HELLO" in new RouterFixture {
        routerRef ! Hello("akka.wamp.realm", dictWithRoles("publisher"))
        expectMsgType[Welcome]
        routerRef ! Goodbye(DictBuilder().withEntry("message", "The host is shutting down now.").build(), "wamp.error.system_shutdown")
        expectMsg(Goodbye(DictBuilder().build(), "wamp.error.goodbye_and_out"))
        router.sessions must have size(0)
      }
    }

    
    "handling subscriptions" should {

      "fail if client says SUBSCRIBE before session open" in new RouterFixture {
        routerRef ! Subscribe(1, emptyDict, "topic1")
        expectMsg(Failure("Session was not open yet."))
      }

      "create a new subscription1 if client1 says SUBSCRIBE to topic1" in new BrokerFixture {
        client1.send(routerRef, Subscribe(1, emptyDict, "topic1"))
        client1.receiveOne(0.seconds) match {
          case Subscribed(request, subscription) =>
            request mustBe 1
            router.subscriptions must have size(1)
            router.subscriptions(subscription) must have (
              'id (subscription),
              'subscribers (Set(client1.ref)),
              'topic ("topic1")
            )
          case _ => fail("Unexpected message")
        } 
      }

      "confirm existing subscription1 any time client1 repeats SUBSCRIBE to topic1" in new BrokerFixture {
        client1.send(routerRef, Subscribe(1, emptyDict, "topic1"))
        client1.receiveOne(0.seconds)
        client1.send(routerRef, Subscribe(2, emptyDict, "topic1"))
        client1.receiveOne(0.seconds) match {
          case Subscribed(request, subscription) =>
            request mustBe 2
            router.subscriptions must have size(1)
            router.subscriptions(subscription) must have (
              'id (subscription),
              'subscribers (Set(client1.ref)),
              'topic ("topic1")
            )
          case _ => fail("Unexpected message")
        }
      }
      
      "create a new subscription2 if client1 says SUBSCRIBE to topic2" in new BrokerFixture {
        client1.send(routerRef, Subscribe(1, emptyDict, "topic1"))
        val id1 = client1.receiveOne(0.seconds).asInstanceOf[Subscribed].subscriptionId
        client1.send(routerRef, Subscribe(2, emptyDict, "topic2"))
        client1.receiveOne(0.seconds) match {
          case Subscribed(_, id2) =>
            router.subscriptions must have size(2)
            router.subscriptions(id1) must have (
              'id (id1),
              'subscribers (Set(client1.ref)),
              'topic ("topic1")
            )
            router.subscriptions(id2) must have (
              'id (id2),
              'subscribers (Set(client1.ref)),
              'topic ("topic2")
            )
          case _ => fail("Unexpected message")
        }
      }

      "update existing subscription1 if also client2 says SUBSCRIBE to topic1" in new BrokerFixture {
        client1.send(routerRef, Subscribe(1, emptyDict, "topic1"))
        client1.receiveOne(1.second)
        client2.send(routerRef, Subscribe(1, emptyDict, "topic1"))
        client2.receiveOne(0.seconds) match {
          case Subscribed(request, subscription) =>
            request mustBe 1
            router.subscriptions must have size(1)
            router.subscriptions(subscription) must have (
              'id (subscription),
              'subscribers (Set(client1.ref, client2.ref)),
              'topic ("topic1")
            )
          case _ => fail("Unexpected message")
        }
      }

      "update existing multiple-subscribers subscription1 if client2 says UNSUBSCRIBE" in new BrokerFixture {
        client1.send(routerRef, Subscribe(1, emptyDict, "topic1"))
        val sid11 = client1.receiveOne(1.second).asInstanceOf[Subscribed].subscriptionId
        client2.send(routerRef, Subscribe(1, emptyDict, "topic1"))
        val sid12 = client2.receiveOne(0.seconds).asInstanceOf[Subscribed].subscriptionId
        sid11 must equal(sid12)
        client2.send(routerRef, Unsubscribe(2, sid12))
        client2.expectMsg(Unsubscribed(2))
        router.subscriptions must have size(1)
        router.subscriptions(sid11) must have (
          'id (sid11),
          'subscribers (Set(client1.ref)),
          'topic ("topic1")
        )
      }
      
      "remove existing single-subscriber subscription2 if client1 says UNSUBSCRIBE" in new BrokerFixture {
        client1.send(routerRef, Subscribe(1, emptyDict, "topic"))
        val sid = client1.receiveOne(0.seconds).asInstanceOf[Subscribed].subscriptionId
        client1.send(routerRef, Unsubscribe(2, sid))
        client1.expectMsg(Unsubscribed(2))
        router.subscriptions must have size(0)  
      }
      
      "reply ERROR if client says UNSUBSCRIBE from unknown subscription" in new BrokerFixture {
        client1.send(routerRef, Unsubscribe(2, 9999))
        client1.expectMsg(Error(UNSUBSCRIBE, 2, DictBuilder().build(), "wamp.error.no_such_subscription"))
      }
    }
  }
  
  
  trait RouterFixture {
    val routerRef = TestActorRef(Router.props(_ + 1))
    val router = routerRef.underlyingActor.asInstanceOf[Router]
    def dictWithRoles(roles: String*) = DictBuilder().withRoles(roles.toSet).build()
    val emptyDict = DictBuilder().build()
  }

  
  trait BrokerFixture extends RouterFixture {
    val client1 = TestProbe("client1")
    client1.send(routerRef , Hello("akka.wamp.realm", dictWithRoles("subscriber")))
    client1.receiveOne(0.seconds)
    val client2 = TestProbe("client2")
    client2.send(routerRef , Hello("akka.wamp.realm", dictWithRoles("subscriber")))
    client2.receiveOne(0.seconds)
  }
}
