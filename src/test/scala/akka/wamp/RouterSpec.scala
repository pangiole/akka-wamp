package akka.wamp

import akka.actor.ActorSystem
import akka.testkit.{TestActorRef, ImplicitSender, TestKit}
import akka.wamp.messages._
import org.scalatest._

class RouterSpec extends TestKit(ActorSystem()) with ImplicitSender with WordSpecLike with MustMatchers {
  "The router" when {
    "handling sessions" should {

      "open a new session first time client says HELLO" in new Fixture {
        routerRef ! Hello("some.realm", Map())
        expectMsgClass(classOf[Welcome])
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
    }
    
    "handling subscriptions" should {
      
      "???" in {
        pending
      }
    }
   }
  
  trait Fixture {
    val routerRef = TestActorRef(Router.props())
    val router = routerRef.underlyingActor.asInstanceOf[Router]
  }
}
