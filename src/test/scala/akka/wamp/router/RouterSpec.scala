package akka.wamp.router

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import akka.testkit._
import akka.wamp.Wamp.Tpe._
import akka.wamp.Wamp._
import org.scalatest._

import scala.concurrent.duration._

class RouterSpec 
  extends TestKit(ActorSystem()) 
    with ImplicitSender 
    with WordSpecLike 
    with MustMatchers 
    with BeforeAndAfter 
    with BeforeAndAfterAll 
{

  implicit val mat = ActorMaterializer()
  var routerRef: TestActorRef[Router] = _
  var routerObj: Router = _
  
  before {
    routerRef = TestActorRef(Router.props(_ + 1))
    routerObj = routerRef.underlyingActor
  }
  
  after {
    system.stop(routerRef)
  }

  override def afterAll() {
    system.terminate()
  }
  
  
  "The router" when {
    "handling transports" should {
      "spawn a new transport actor on incoming connection" in new RouterFixture {
        pending
        val flow: Flow[HttpResponse, HttpRequest, NotUsed] = ???
        routerRef ! Http.IncomingConnection(null, null, flow)
        routerRef.children must have size(1)
        // TODO routerRef.children.head mustBe actorOf[Transport] 
      }
    }
    
    "handling sessions" should {
      "reply ABORT if client says HELLO for unknown realm" in new RouterFixture {
        routerRef ! Hello("unknown.realm", Dict().withRoles("publisher"))
        expectMsg(Abort(Dict("message" -> "The realm unknown.realm does not exist."), "wamp.error.no_such_realm"))
        routerObj.realms must have size(1)
        routerObj.realms must contain only ("akka.wamp.realm")
        routerObj.sessions mustBe empty
      }

      "auto-create realm if client says HELLO for unknown realm" in new RouterFixture {
        // TODO set different ActorSystem properties
        pending
        routerRef ! Hello("myapp.realm", Dict().withRoles("publisher"))
        expectMsgType[Welcome]
        routerObj.realms must have size(2)
        routerObj.realms must contain ("akka.wamp.realm", "myapp.realm")
      }

      
      "reply WELCOME if client says HELLO for existing realm" in new RouterFixture {
        routerRef ! Hello("akka.wamp.realm", Dict().withRoles("publisher"))
        expectMsg(Welcome(0, Dict().withRoles("broker").withAgent("akka-wamp-0.2.0")))
        routerObj.realms must have size(1)
        routerObj.realms must contain only ("akka.wamp.realm")
        routerObj.sessions must have size(1)
        val session = routerObj.sessions(0)
        session must have (
          'id (0),
          'transport (testActor),
          'roles (Set("publisher")),
          'realm ("akka.wamp.realm")
        )
      }

      
      "fail if client says HELLO twice (regardless the realm)" in new RouterFixture {
        routerRef ! Hello("akka.wamp.realm", Dict().withRoles("publisher"))
        receiveOne(0.seconds)
        routerRef ! Hello("whatever.realm", Dict().withRoles("publisher"))
        expectMsg(Failure("Session was already open."))
        routerObj.sessions  mustBe empty
      }

      // TODO WAMP spec doesn't clarify if client can open a second connection attached to a different realm?
      
      
      "fail if client says GOODBYE before HELLO" in new RouterFixture {
        routerRef ! Goodbye(Dict(), "whatever.reason")
        expectMsg(Failure("Session was not open yet."))
      }
      
      
      "reply GOODBYE if client says GOODBYE after HELLO" in new RouterFixture {
        routerRef ! Hello("akka.wamp.realm", Dict().withRoles("publisher"))
        expectMsgType[Welcome]
        routerRef ! Goodbye(Dict("message" -> "The host is shutting down now."), "wamp.error.system_shutdown")
        expectMsg(Goodbye(Dict(), "wamp.error.goodbye_and_out"))
        routerObj.sessions  mustBe empty
      }
    }
    
    
    
    "handling publications" should {
      
      "fail if client says PUBLISH before session has opened" in new RouterFixture {
        routerRef ! Publish(0, Dict(), "topic1")
        expectMsg(Failure("Session was not open yet."))
        expectNoMsg()
        routerObj.publications mustBe empty
        
      }

      "reply ERROR if client has no publisher role" in new BrokerFixture {
        client1.send(routerRef, Publish(0, ackDict, "topic1"))
        client1.expectMsg(Error(PUBLISH, 0, Dict(), "akka.wamp.error.no_publisher_role"))
        client1.expectNoMsg()
        routerObj.publications mustBe empty
      }
      
      "reply ERROR if client says PUBLISH to a topic with no subscribers" in new BrokerFixture {
        client3.send(routerRef, Publish(1, ackDict, "topic1"))
        client3.expectMsg(Error(PUBLISH, 1, Dict(), "wamp.error.no_such_topic"))
        client3.expectNoMsg()
        routerObj.publications mustBe empty
      }
      
      "dispatch EVENT if client says PUBLISH to a topic with subscribers" in new BrokerFixture {
        client1.send(routerRef, Subscribe(0, Dict(), "topic1")); client1.receiveOne(1.second)
        client2.send(routerRef, Subscribe(0, Dict(), "topic1"));client2.receiveOne(0.seconds)
        client3.send(routerRef, Publish(0, ackDict, "topic1", Some(Payload(List(44.23,"paolo",null,true)))))
        client1.expectMsg(Event(0, 0, Dict(), Some(Payload(List(44.23,"paolo",null,true)))))
        client2.expectMsg(Event(0, 0, Dict(), Some(Payload(List(44.23,"paolo",null,true)))))
        client3.expectMsg(Published(0, 0))
        client3.expectNoMsg()
      }
    }

    
    
    "handling subscriptions" should {
      
      "fail if client says SUBSCRIBE before session has opened" in new RouterFixture {
        routerRef ! Subscribe(0, Dict(), "topic1")
        expectMsg(Failure("Session was not open yet."))
        expectNoMsg()
      }
      
      "reply ERROR if client has no subscriber role" in new BrokerFixture {
        client3.send(routerRef, Subscribe(0, Dict(), "topic1"))
        client3.expectMsg(Error(SUBSCRIBE, 0, Dict(), "akka.wamp.error.no_subscriber_role"))
        routerObj.subscriptions mustBe empty
        client3.expectNoMsg()
      }
      
      "create a new subscription1 if client1 says SUBSCRIBE to topic1" in new BrokerFixture {
        client1.send(routerRef, Subscribe(0, Dict(), "topic1"))
        client1.receiveOne(0.seconds) match {
          case Subscribed(requestId, subscriptionId) =>
            requestId mustBe 0
            routerObj.subscriptions must have size(1)
            routerObj.subscriptions(subscriptionId) must have (
              'id (subscriptionId),
              'subscribers (Set(client1.ref)),
              'topic ("topic1")
            )
          case _ => fail("Unexpected message")
        } 
      }

      "confirm existing subscription1 any time client1 repeats SUBSCRIBE to topic1" in new BrokerFixture {
        client1.send(routerRef, Subscribe(0, Dict(), "topic1"))
        client1.receiveOne(0.seconds)
        client1.send(routerRef, Subscribe(1, Dict(), "topic1"))
        client1.receiveOne(0.seconds) match {
          case Subscribed(requestId, subscriptionId) =>
            requestId mustBe 1
            routerObj.subscriptions must have size(1)
            routerObj.subscriptions(subscriptionId) must have (
              'id (subscriptionId),
              'subscribers (Set(client1.ref)),
              'topic ("topic1")
            )
          case _ => fail("Unexpected message")
        }
      }
      
      "create a new subscription2 if client1 says SUBSCRIBE to topic2" in new BrokerFixture {
        client1.send(routerRef, Subscribe(0, Dict(), "topic1"))
        val id1 = client1.receiveOne(0.seconds).asInstanceOf[Subscribed].subscriptionId
        client1.send(routerRef, Subscribe(1, Dict(), "topic2"))
        client1.receiveOne(0.seconds) match {
          case Subscribed(_, id2) =>
            routerObj.subscriptions must have size(2)
            routerObj.subscriptions(id1) must have (
              'id (id1),
              'subscribers (Set(client1.ref)),
              'topic ("topic1")
            )
            routerObj.subscriptions(id2) must have (
              'id (id2),
              'subscribers (Set(client1.ref)),
              'topic ("topic2")
            )
          case _ => fail("Unexpected message")
        }
      }

      "update existing subscription1 if also client2 says SUBSCRIBE to topic1" in new BrokerFixture {
        client1.send(routerRef, Subscribe(0, Dict(), "topic1"))
        client1.receiveOne(1.second)
        client2.send(routerRef, Subscribe(0, Dict(), "topic1"))
        client2.receiveOne(0.seconds) match {
          case Subscribed(requestId, subscriptionId) =>
            requestId mustBe 0
            routerObj.subscriptions must have size(1)
            routerObj.subscriptions(subscriptionId) must have (
              'id (subscriptionId),
              'subscribers (Set(client1.ref, client2.ref)),
              'topic ("topic1")
            )
          case _ => fail("Unexpected message")
        }
      }

      "update existing multiple-subscribers subscription1 if client2 says UNSUBSCRIBE" in new BrokerFixture {
        client1.send(routerRef, Subscribe(0, Dict(), "topic1"))
        val sid11 = client1.receiveOne(1.second).asInstanceOf[Subscribed].subscriptionId
        client2.send(routerRef, Subscribe(0, Dict(), "topic1"))
        val sid12 = client2.receiveOne(0.seconds).asInstanceOf[Subscribed].subscriptionId
        sid11 must equal(sid12)
        client2.send(routerRef, Unsubscribe(1, sid12))
        client2.expectMsg(Unsubscribed(1))
        routerObj.subscriptions must have size(1)
        routerObj.subscriptions(sid11) must have (
          'id (sid11),
          'subscribers (Set(client1.ref)),
          'topic ("topic1")
        )
      }
      
      "remove existing single-subscriber subscription2 if client1 says UNSUBSCRIBE" in new BrokerFixture {
        client1.send(routerRef, Subscribe(0, Dict(), "topic"))
        val sid = client1.receiveOne(0.seconds).asInstanceOf[Subscribed].subscriptionId
        client1.send(routerRef, Unsubscribe(1, sid))
        client1.expectMsg(Unsubscribed(1))
        routerObj.subscriptions  mustBe empty  
      }
      
      "reply ERROR if client says UNSUBSCRIBE from unknown subscription" in new BrokerFixture {
        client1.send(routerRef, Unsubscribe(0, 9999))
        client1.expectMsg(Error(UNSUBSCRIBE, 0, Dict(), "wamp.error.no_such_subscription"))
      }
    }
  }
  
  
  trait RouterFixture {
    
  }

  
  trait BrokerFixture extends RouterFixture {
    val client1 = TestProbe("client1")
    client1.send(routerRef , Hello("akka.wamp.realm", Dict().withRoles("subscriber")))
    client1.receiveOne(0.seconds)
    val client2 = TestProbe("client2")
    client2.send(routerRef , Hello("akka.wamp.realm", Dict().withRoles("subscriber","publisher")))
    client2.receiveOne(0.seconds)
    val client3 = TestProbe("client3")
    client3.send(routerRef , Hello("akka.wamp.realm", Dict().withRoles("publisher")))
    client3.receiveOne(0.seconds)
    val ackDict = Dict("acknowledge" -> true)
  }
}
