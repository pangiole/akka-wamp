package akka.wamp

import akka.actor.ActorSystem
import akka.testkit.{TestProbe, ImplicitSender, TestActorRef, TestKit}
import akka.wamp.Router.ProtocolError
import akka.wamp.messages._
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
        expectMsg(Welcome(1L, DictBuilder().withRoles(Set("broker")).withEntry("agent", "akka-wamp-0.1.0").build()))
        router.realms must have size(1)
        router.realms must contain only ("akka.wamp.realm")
        router.sessions must have size(1)
        val session = router.sessions(1L)
        session must have (
          'id (1L),
          'routerRef (routerRef),
          'routerRoles (Set("broker")),
          'clientRef (testActor),
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
        routerRef ! Subscribe(713845233L, emptyDict, "com.myapp.mytopic1")
        expectMsg(ProtocolError("Session was not open yet."))
      }

      "reply new SUBSCRIBED first time client C1 says SUBSCRIBE to topic T1" in new BrokerFixture {
        val topic1 = "com.myapp.mytopic1"
        val sid1 = topic1.hashCode
        client1.send(routerRef, Subscribe(713845233L, emptyDict, topic1))
        client1.expectMsg(Subscribed(713845233L, sid1))
        router.subscriptions must have size(1)
        router.subscriptions(sid1) must have (
          'id (sid1),
          'clientRefs (Set(client1.ref)),
          'topic (topic1)
        )
      }

      "reply existing SUBSCRIBED second time client C1 says SUBSCRIBE to topic T1" in new BrokerFixture {
        val topic1 = "com.myapp.mytopic1"
        client1.send(routerRef, Subscribe(713845233L, emptyDict, topic1))
        val sid1 = topic1.hashCode
        client1.expectMsg(Subscribed(713845233L, sid1))
        client1.send(routerRef, Subscribe(938843243L, emptyDict, topic1))
        client1.expectMsg(Subscribed(938843243L, sid1))
        router.subscriptions must have size(1)
        router.subscriptions(sid1) must have (
          'id (sid1),
          'clientRefs (Set(client1.ref)),
          'topic (topic1)
        )
      }
      
      "reply new SUBSCRIBED if client C1 says SUBSCRIBE to topic T2" in new BrokerFixture {
        val topic1 = "com.myapp.mytopic1"
        val sid1 = topic1.hashCode
        client1.send(routerRef, Subscribe(713845233L, emptyDict, topic1))
        client1.expectMsg(Subscribed(713845233L, sid1))
        val topic2 = "com.myapp.mytopic2"
        val sid2 = topic2.hashCode
        client1.send(routerRef, Subscribe(1398123L, emptyDict, topic2))
        client1.expectMsg(Subscribed(1398123L, sid2))
        router.subscriptions must have size(2)
        router.subscriptions(sid1) must have (
          'id (sid1),
          'clientRefs (Set(client1.ref)),
          'topic (topic1)
        )
        router.subscriptions(sid2) must have (
          'id (sid2),
          'clientRefs (Set(client1.ref)),
          'topic (topic2)
        )
      }

      "reply existing SUBSCRIBED if client C2 says SUBSCRIBE to topic T1" in new BrokerFixture {
        val topic1 = "com.myapp.mytopic1"
        val sid1 = topic1.hashCode
        client1.send(routerRef, Subscribe(713845233L, emptyDict, topic1))
        client1.receiveOne(1.second)
        client2.send(routerRef, Subscribe(991233343L, emptyDict, topic1))
        client2.expectMsg(Subscribed(991233343L, sid1))
        router.subscriptions must have size(1)
        router.subscriptions(sid1) must have (
          'id (sid1),
          'clientRefs (Set(client1.ref, client2.ref)),
          'topic (topic1)
        )
      }
    }
  }
  
  
  trait RouterFixture {
    var sid = 0L
    def generateProgressiveSessionId = (map: Map[Id, Any], id: Id) => { sid = sid + 1; sid}  
    val routerRef = TestActorRef(Router.props(generateProgressiveSessionId))
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
