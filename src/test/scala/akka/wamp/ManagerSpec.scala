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

  "The IO(Wamp) manager" should "bind a router" in { f =>
    val manager = IO(Wamp)
    manager ! Bind(f.listener.ref)
    val bound = f.listener.expectMsgType[Bound](16 seconds)
    bound.url must startWith("ws://127.0.0.1:")
    bound.url must endWith("/ws")
  }

  it should "unbind a router" in { fixture =>
    pending
  }
  
  it should "connect a client" in { f =>
    val manager = IO(Wamp)
    manager ! Bind(f.router)
    val bound = f.listener.expectMsgType[Bound](16 seconds)
    
    manager ! Connect(client = testActor, bound.url)
    val connected = expectMsgType[Wamp.Connected](16 seconds)
    connected.peer must not be (null)
  }
  
  
  it should "disconnect a client" in { f =>
    // TODO https://github.com/angiolep/akka-wamp/issues/29
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
