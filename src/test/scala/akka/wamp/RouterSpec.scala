package akka.wamp

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import akka.wamp.Router.ProtocolError
import akka.wamp.messages._
import org.scalatest._

class RouterSpec extends TestKit(ActorSystem()) with ImplicitSender with WordSpecLike with MustMatchers {
  "The router" when {
    "handling sessions" should {

      "reply WELCOME if client says HELLO for existing realm" in new Fixture {
        routerRef ! Hello("akka.wamp.realm", DictBuilder().withRoles("publisher").build())
        expectMsg(Welcome(0L, DictBuilder().withRoles("broker").withEntry("agent", "akka-wamp-0.1.0").build()))
        router.realms must have size(1)
        router.realms must contain only ("akka.wamp.realm")
        router.sessions must have size(1)
        val sid = router.sessions.keySet.head
        val session = router.sessions(sid)
        session must have (
          'id (sid),
          'peer1 (routerRef),
          'peer2 (testActor),
          'realm ("akka.wamp.realm")
        )
      }

      "reply ABORT if client says HELLO for unknown realm" in new Fixture {
        routerRef ! Hello("unknown.realm", DictBuilder().withRoles("whatever.role").build())
        expectMsg(Abort(DictBuilder().withEntry("message", "The realm unknown.realm does not exist.").build(), "wamp.error.no_such_realm"))
        router.realms must have size(1)
        router.realms must contain only ("akka.wamp.realm")
        router.sessions mustBe empty
      }
      
      
      "auto-create realm if client says HELLO for unknown realm" in {
        pending
      }
      

      "protocol error if client says HELLO twice regardless the realm" in new Fixture {
        routerRef ! Hello("akka.wamp.realm", DictBuilder().withRoles("publisher").build())
        expectMsgType[Welcome]
        routerRef ! Hello("whatever.realm", DictBuilder().withRoles("whatever.role").build())
        expectMsg(ProtocolError("Session already open"))
        router.sessions must have size(0)
      }

      // TODO WAMP specs don't clarify if client can open a second connection attached to a different realm?
      
      
      "protocol error if client says GOODBYE before HELLO" in new Fixture {
        routerRef ! Goodbye(DictBuilder().build(), "whatever.reason")
        expectMsg(ProtocolError("No session was open"))
      }
      
      "reply GOODBYE if client says GOODBYE after HELLO" in new Fixture {
        routerRef ! Hello("akka.wamp.realm", DictBuilder().withRoles("publisher").build())
        expectMsgType[Welcome]
        routerRef ! Goodbye(DictBuilder().withEntry("message", "The host is shutting down now.").build(), "wamp.error.system_shutdown")
        expectMsg(Goodbye(DictBuilder().build(), "wamp.error.goodbye_and_out"))
        router.sessions must have size(0)
      }

      
    }
    
    "handling subscriptions" should {
      
      "???" in {
        pending
      }
    }
   }
  
  trait Fixture {
    def fakegen = (m: Map[Long, _]) => 0L 
    val routerRef = TestActorRef(Router.props(fakegen))
    val router = routerRef.underlyingActor.asInstanceOf[Router]
  }
}
