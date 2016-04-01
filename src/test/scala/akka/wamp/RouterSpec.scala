package akka.wamp

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
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

      
      "protocol error if client says HELLO twice (regardless the realm)" in new RouterFixture {
        routerRef ! Hello("akka.wamp.realm", dictWithRoles("publisher"))
        receiveOne(0.seconds)
        routerRef ! Hello("whatever.realm", dictWithRoles("whatever.role"))
        expectMsg(ProtocolError("Session was already open."))
        router.sessions must have size(0)
      }

      // TODO WAMP specs don't clarify if client can open a second connection attached to a different realm?
      
      
      "protocol error if client says GOODBYE before HELLO" in new RouterFixture {
        routerRef ! Goodbye(DictBuilder().build(), "whatever.reason")
        expectMsg(ProtocolError("Session was not open yet."))
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

      "protocol error if client says SUBSCRIBE before HELLO" in new RouterFixture {
        routerRef ! Subscribe(1, emptyDict, "topic1")
        expectMsg(ProtocolError("Session was not open yet."))
      }

      "create new subscription to topic1 first time client1 subscribes to it" in new BrokerFixture {
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

      "confirm existing subscription to topic1 subsequent times client1 re-subscribes to it" in new BrokerFixture {
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
      
      "create new subscription to topic2 if client1 subscribes to it after having subscribed to topic1" in new BrokerFixture {
        client1.send(routerRef, Subscribe(1, emptyDict, "topic1"))
        val subscription1 = client1.receiveOne(0.seconds).asInstanceOf[Subscribed].subscriptionId
        client1.send(routerRef, Subscribe(2, emptyDict, "topic2"))
        client1.receiveOne(0.seconds) match {
          case Subscribed(_, subscription2) =>
            router.subscriptions must have size(2)
            router.subscriptions(subscription1) must have (
              'id (subscription1),
              'subscribers (Set(client1.ref)),
              'topic ("topic1")
            )
            router.subscriptions(subscription2) must have (
              'id (subscription2),
              'subscribers (Set(client1.ref)),
              'topic ("topic2")
            )
          case _ => fail("Unexpected message")
        }
      }

      "update existing subscription to topic1 if also client2 subscribes to it" in new BrokerFixture {
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
      
      "unsubscribe from topic1 if client1 unsubscribe from it after having subscribed to it" in new BrokerFixture {
        client1.send(routerRef, Subscribe(1, emptyDict, "topic1"))
        val subscription = client1.receiveOne(0.seconds).asInstanceOf[Subscribed].subscriptionId
        client1.send(routerRef, Unsubscribe(2, subscription))
        client1.expectMsg(Unsubscribed(2))
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
