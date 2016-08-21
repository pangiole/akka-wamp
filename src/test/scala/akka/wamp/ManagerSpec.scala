package akka.wamp

import akka.actor._
import akka.io._
import akka.testkit.{TestActorRef, TestProbe}
import akka.wamp.Wamp._
import akka.wamp.router.Router
import org.scalatest.ParallelTestExecution
import scala.concurrent.duration._


// reference.conf is overriding akka.wamp.router.port to enable dynamic port bindings
class ManagerSpec 
  extends ActorSpec(ActorSystem("test"))
  with ParallelTestExecution
{

  "The IO(Wamp) manager" should "bind router" in { fixture =>
    val manager = IO(Wamp)
    manager ! Bind(fixture.listener.ref)
    val bound = fixture.listener.expectMsgType[Bound](16 seconds)
    bound.url must startWith("ws://127.0.0.1:")
    bound.url must endWith("/ws")
  }
  
  
  it should "connect client" in { fixture =>
    val manager = IO(Wamp)
    manager ! Bind(fixture.router)
    val bound = fixture.listener.expectMsgType[Bound](16 seconds)
    
    manager ! Connect(client = testActor, bound.url)
    val connected = expectMsgType[Wamp.Connected](16 seconds)
    connected.peer must not be (null)
  }
  
  
  it should "unbind router" in { fixture =>
    pending
  }

  
  it should "disconnect client" in { fixture =>
    pending
  }

  
  // see http://www.scalatest.org/user_guide/sharing_fixtures#withFixtureOneArgTest
  case class FixtureParam(router: TestActorRef[Router], listener: TestProbe)
  
  def withFixture(test: OneArgTest) = {
    val listener = TestProbe()
    val router = TestActorRef[Router](Router.props(listener = Some(listener.ref)))
    val theFixture = FixtureParam(router, listener)
    try {
      withFixture(test.toNoArgTest(theFixture)) 
    }
    finally {
      system.stop(listener.ref)
      system.stop(router)
    }
  }
}
