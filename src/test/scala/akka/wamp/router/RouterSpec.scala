package akka.wamp.router

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import akka.testkit._
import akka.wamp._
import akka.wamp.Wamp._
import akka.wamp.messages._
import akka.wamp.Tpe._
import org.scalatest._

import scala.concurrent.duration._

class RouterSpec  extends TestKit(ActorSystem()) 
  with WordSpecLike with MustMatchers with BeforeAndAfterAll
  with ImplicitSender  with RouterFixture
{
  implicit val mat = ActorMaterializer()

  def withRouter(testCode: (TestActorRef[Router]) => Any) {
    val router = TestActorRef[Router](Router.props(scopes))
    try {
      testCode(router)
    } finally {
      system.stop(router)
    }
  }
  
  override def afterAll() {
    system.terminate()
  }
  

  "The router" when {
    "handling transports" should {
      "spawn a new transport actor on incoming connection" in withRouter { router =>
        pending
        val flow: Flow[HttpResponse, HttpRequest, NotUsed] = ???
        router ! Http.IncomingConnection(null, null, flow)
        router.children must have size(1)
        // TODO router.children.head mustBe actorOf[Transport] 
      }
    }
    
    "handling sessions" should {
      "reply ABORT if client says HELLO for unknown realm" in withRouter { router =>
        pending
        // TODO disable auto-create-realm to test this scenario
        router ! Hello("unknown.realm", Dict().withRoles("publisher"))
        expectMsg(Abort(Dict("message" -> "The realm unknown.realm does not exist."), "wamp.error.no_such_realm"))
        router.underlyingActor.realms must have size(1)
        router.underlyingActor.realms must contain only ("akka.wamp.realm")
        router.underlyingActor.sessions mustBe empty
      }

      "auto-create realm if client says HELLO for unknown realm" in withRouter { router =>
        // TODO set different ActorSystem properties
        pending
        router ! Hello("myapp.realm", Dict().withRoles("publisher"))
        expectMsgType[Welcome]
        router.underlyingActor.realms must have size(2)
        router.underlyingActor.realms must contain ("akka.wamp.realm", "myapp.realm")
      }

      
      "reply WELCOME if client says HELLO for existing realm" in withRouter { router =>
        router ! Hello("akka.wamp.realm", Dict().withRoles("publisher"))
        expectMsg(Welcome(1, Dict().withRoles("broker").withAgent("akka-wamp-0.3.0")))
        router.underlyingActor.realms must have size(1)
        router.underlyingActor.realms must contain only ("akka.wamp.realm")
        router.underlyingActor.sessions must have size(1)
        val session = router.underlyingActor.sessions(1)
        session must have (
          'id (1),
          'transport (testActor),
          'roles (Set("publisher")),
          'realm ("akka.wamp.realm")
        )
      }

      
      "fail if client says HELLO twice (regardless the realm)" in withRouter { router =>
        router ! Hello("akka.wamp.realm", Dict().withRoles("publisher"))
        receiveOne(0.seconds)
        router ! Hello("whatever.realm", Dict().withRoles("publisher"))
        expectMsg(Failure("Session was already open."))
        router.underlyingActor.sessions  mustBe empty
      }

      // TODO WAMP spec doesn't clarify if client can open a second connection attached to a different realm?
      
      
      "fail if client says GOODBYE before HELLO" in withRouter { router =>
        router ! Goodbye()
        expectMsg(Failure("Session was not open yet."))
      }
      
      
      "reply GOODBYE if client says GOODBYE after HELLO" in withRouter { router =>
        router ! Hello("akka.wamp.realm", Dict().withRoles("publisher"))
        expectMsgType[Welcome]
        router ! Goodbye("wamp.error.system_shutdown", Dict("message" -> "The host is shutting down now."))
        expectMsg(Goodbye("wamp.error.goodbye_and_out", Dict()))
        router.underlyingActor.sessions  mustBe empty
      }
    }
    
    
    
    "handling publications" should {
      
      "fail if client says PUBLISH before session has opened" in withRouter { router =>
        router ! Publish(1, "topic1", options = Dict())
        expectMsg(Failure("Session was not open yet."))
        expectNoMsg()
        router.underlyingActor.publications mustBe empty
        
      }

      "reply ERROR if client has no publisher role" in withRouter { router =>
        val client = TestProbe("client")
        client.send(router , Hello("akka.wamp.realm", Dict().withRoles("subscriber")))
        client.receiveOne(0.seconds)
        client.send(router, Publish(1, "topic1", options = Dict("acknowledge" -> true)))
        client.expectMsg(Error(PUBLISH, 1, Dict(), "akka.wamp.error.no_publisher_role"))
        client.expectNoMsg()
        router.underlyingActor.publications mustBe empty
      }
      
      "reply ERROR if client says PUBLISH to a topic with no subscribers" in withRouter { router =>
        val client = TestProbe("client")
        client.send(router , Hello("akka.wamp.realm", Dict().withRoles("publisher")))
        client.receiveOne(0.seconds)
        client.send(router, Publish(1, "topic1", options = Dict("acknowledge" -> true)))
        client.expectMsg(Error(PUBLISH, 1, Dict(), "wamp.error.no_such_topic"))
        client.expectNoMsg()
        router.underlyingActor.publications mustBe empty
      }
      
      "dispatch EVENT if client says PUBLISH to a topic with subscribers" in withRouter { router =>
        val client1 = TestProbe("client1")
        client1.send(router , Hello("akka.wamp.realm", Dict().withRoles("subscriber")))
        client1.receiveOne(0.seconds)
        val client2 = TestProbe("client2")
        client2.send(router , Hello("akka.wamp.realm", Dict().withRoles("subscriber","publisher")))
        client2.receiveOne(0.seconds)
        val client3 = TestProbe("client3")
        client3.send(router , Hello("akka.wamp.realm", Dict().withRoles("publisher")))
        client3.receiveOne(0.seconds)
        
        client1.send(router, Subscribe(1, "topic1", Dict())); client1.receiveOne(1.second)
        client2.send(router, Subscribe(1, "topic1", Dict()));client2.receiveOne(0.seconds)
        client3.send(router, Publish(1, "topic1", Some(Payload(List(44.23,"paolo",null,true))), Dict("acknowledge" -> true)))
        client1.expectMsg(Event(1, 1, Dict(), Some(Payload(List(44.23,"paolo",null,true)))))
        client2.expectMsg(Event(1, 1, Dict(), Some(Payload(List(44.23,"paolo",null,true)))))
        client3.expectMsg(Published(1, 1))
        client3.expectNoMsg()
      }
    }

    
    
    "handling subscriptions" should {
      
      "fail if client says SUBSCRIBE before session has opened" in withRouter { router =>
        router ! Subscribe(1, "topic1", Dict())
        expectMsg(Failure("Session was not open yet."))
        expectNoMsg()
      }
      
      "reply ERROR if client has no subscriber role" in withRouter { router =>
        val client = TestProbe("client")
        client.send(router , Hello("akka.wamp.realm", Dict().withRoles("publisher")))
        client.receiveOne(0.seconds)
        client.send(router, Subscribe(1, "topic1", Dict()))
        client.expectMsg(Error(SUBSCRIBE, 1, Dict(), "akka.wamp.error.no_subscriber_role"))
        router.underlyingActor.subscriptions mustBe empty
        client.expectNoMsg()
      }
      
      "create a new subscription1 if client1 says SUBSCRIBE to topic1" in withRouter { router =>
        val client = TestProbe("client")
        client.send(router , Hello("akka.wamp.realm", Dict().withRoles("subscriber")))
        client.receiveOne(0.seconds)
        client.send(router, Subscribe(1, "topic1", Dict()))
        client.receiveOne(0.seconds) match {
          case Subscribed(requestId, subscriptionId) =>
            requestId mustBe 1
            router.underlyingActor.subscriptions must have size(1)
            router.underlyingActor.subscriptions(subscriptionId) must have (
              'id (subscriptionId),
              'subscribers (Set(client.ref)),
              'topic ("topic1")
            )
          case _ => fail("Unexpected message")
        } 
      }

      "confirm existing subscription1 any time client1 repeats SUBSCRIBE to topic1" in withRouter { router =>
        val client = TestProbe("client")
        client.send(router , Hello("akka.wamp.realm", Dict().withRoles("subscriber")))
        client.receiveOne(0.seconds)
        client.send(router, Subscribe(1, "topic1", Dict()))
        client.receiveOne(0.seconds)
        client.send(router, Subscribe(2, "topic1", Dict()))
        client.receiveOne(0.seconds) match {
          case Subscribed(requestId, subscriptionId) =>
            requestId mustBe 2
            router.underlyingActor.subscriptions must have size(1)
            router.underlyingActor.subscriptions(subscriptionId) must have (
              'id (subscriptionId),
              'subscribers (Set(client.ref)),
              'topic ("topic1")
            )
          case _ => fail("Unexpected message")
        }
      }
      
      "create a new subscription2 if client1 says SUBSCRIBE to topic2" in withRouter { router =>
        val client = TestProbe("client")
        client.send(router , Hello("akka.wamp.realm", Dict().withRoles("subscriber")))
        client.receiveOne(0.seconds)
        client.send(router, Subscribe(1, "topic1", Dict()))
        val id1 = client.receiveOne(0.seconds).asInstanceOf[Subscribed].subscriptionId
        client.send(router, Subscribe(2, "topic2", Dict()))
        client.receiveOne(0.seconds) match {
          case Subscribed(_, id2) =>
            router.underlyingActor.subscriptions must have size(2)
            router.underlyingActor.subscriptions(id1) must have (
              'id (id1),
              'subscribers (Set(client.ref)),
              'topic ("topic1")
            )
            router.underlyingActor.subscriptions(id2) must have (
              'id (id2),
              'subscribers (Set(client.ref)),
              'topic ("topic2")
            )
          case _ => fail("Unexpected message")
        }
      }

      "update existing subscription1 if also client2 says SUBSCRIBE to topic1" in withRouter { router =>
        val client1 = TestProbe("client1")
        client1.send(router , Hello("akka.wamp.realm", Dict().withRoles("subscriber")))
        client1.receiveOne(0.seconds)
        client1.send(router, Subscribe(1, "topic1", Dict()))
        client1.receiveOne(0.second)
        val client2 = TestProbe("client2")
        client2.send(router , Hello("akka.wamp.realm", Dict().withRoles("subscriber","publisher")))
        client2.receiveOne(0.seconds)
        client2.send(router, Subscribe(2, "topic1", Dict()))
        client2.receiveOne(0.seconds) match {
          case Subscribed(requestId, subscriptionId) =>
            requestId mustBe 2
            router.underlyingActor.subscriptions must have size(1)
            router.underlyingActor.subscriptions(subscriptionId) must have (
              'id (subscriptionId),
              'subscribers (Set(client1.ref, client2.ref)),
              'topic ("topic1")
            )
          case _ => fail("Unexpected message")
        }
      }

      "update existing multiple-subscribers subscription1 if client2 says UNSUBSCRIBE" in withRouter { router =>
        val client1 = TestProbe("client1")
        client1.send(router , Hello("akka.wamp.realm", Dict().withRoles("subscriber")))
        client1.receiveOne(0.seconds)
        client1.send(router, Subscribe(1, "topic1", Dict()))
        val sid11 = client1.receiveOne(0.second).asInstanceOf[Subscribed].subscriptionId
        val client2 = TestProbe("client2")
        client2.send(router , Hello("akka.wamp.realm", Dict().withRoles("subscriber","publisher")))
        client2.receiveOne(0.seconds)
        client2.send(router, Subscribe(2, "topic1", Dict()))
        val sid12 = client2.receiveOne(0.seconds).asInstanceOf[Subscribed].subscriptionId
        sid11 must equal(sid12)
        client2.send(router, Unsubscribe(3, sid12))
        client2.expectMsg(Unsubscribed(3))
        router.underlyingActor.subscriptions must have size(1)
        router.underlyingActor.subscriptions(sid11) must have (
          'id (sid11),
          'subscribers (Set(client1.ref)),
          'topic ("topic1")
        )
      }
      
      "remove existing single-subscriber subscription2 if client1 says UNSUBSCRIBE" in withRouter { router =>
        val client = TestProbe("client1")
        client.send(router , Hello("akka.wamp.realm", Dict().withRoles("subscriber")))
        client.receiveOne(0.seconds)
        client.send(router, Subscribe(1, "topic", Dict()))
        val sid = client.receiveOne(0.seconds).asInstanceOf[Subscribed].subscriptionId
        client.send(router, Unsubscribe(2, sid))
        client.expectMsg(Unsubscribed(2))
        router.underlyingActor.subscriptions  mustBe empty  
      }
      
      "reply ERROR if client says UNSUBSCRIBE from unknown subscription" in withRouter { router =>
        val client = TestProbe("client1")
        client.send(router , Hello("akka.wamp.realm", Dict().withRoles("subscriber")))
        client.receiveOne(0.seconds)
        client.send(router, Unsubscribe(1, 9999))
        client.expectMsg(Error(UNSUBSCRIBE, 1, Dict(), "wamp.error.no_such_subscription"))
      }
    }
  }
  
}
