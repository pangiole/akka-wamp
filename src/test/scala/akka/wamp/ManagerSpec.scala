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

  "The IO(Wamp) manager" should "bind router" in { f =>
    val manager = IO(Wamp)
    manager ! Bind(f.listener.ref)
    val bound = f.listener.expectMsgType[Bound](8 seconds)
    bound.url must startWith("ws://127.0.0.1:")
    bound.url must endWith("/ws")
  }
  
  
  it should "connect client" in { f =>
    val manager = IO(Wamp)
    manager ! Bind(f.router)
    val bound = f.listener.expectMsgType[Bound](8 seconds)
    
    manager ! Connect(client = testActor, bound.url)
    val connected = expectMsgType[Wamp.Connected](8 seconds)
    connected.peer must not be (null)
  }
  
  
  it should "unbind router" in { f =>
    pending
  }

  
  it should "disconnect client" in { f =>
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
