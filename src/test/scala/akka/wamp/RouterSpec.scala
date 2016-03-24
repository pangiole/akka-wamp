package akka.wamp

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import akka.wamp.Router.ProtocolError
import akka.wamp.messages._
import org.scalatest._

class RouterSpec extends TestKit(ActorSystem()) with ImplicitSender with WordSpecLike with MustMatchers {
  "The router" when {
    "handling sessions" should {

      "open a new session first time client says HELLO" in new Fixture {
        routerRef ! Hello("some.realm", Map())
        expectMsg(Welcome(0L, Dict.withRoles("broker")))
        router.sessions must have size(1)
        val sid = router.sessions.keySet.head
        val session = router.sessions(sid)
        session must have (
          'id (sid),
          'peer1 (routerRef),
          'peer2 (testActor),
          'realm ("some.realm")
        )
      }

      "protocol error if client says HELLO twice" in new Fixture {
        routerRef ! Hello("some.realm", Map())
        expectMsgClass(classOf[Welcome])
        routerRef ! Hello("some.realm", Map())
        expectMsg(ProtocolError("Session already open"))
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
