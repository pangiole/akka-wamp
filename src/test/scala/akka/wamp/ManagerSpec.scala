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
    manager ! Bind(f.router)
    val signal = expectMsgType[Bound](16 seconds)
    signal.url must startWith("ws://127.0.0.1:")
    signal.url must endWith("/router")
  }

  
  it should "unbind a transport listener" in { f =>
    val manager = IO(Wamp)
    manager ! Bind(f.router)
    val signal = expectMsgType[Bound](16 seconds)
    val listener = signal.listener
    
    val watcher = TestProbe("watchee")
    watcher.watch(listener)
    
    listener ! Unbind
    expectNoMsg()
    val terminated = watcher.expectMsgType[Terminated]
    terminated.actor mustBe listener
  }
  
  
  it should "connect a client" in { f =>
    val manager = IO(Wamp)
    manager ! Bind(f.router)
    val signal = expectMsgType[Bound](16 seconds)
    
    manager ! Connect(signal.url, "wamp.2.json")
    val connected = expectMsgType[Wamp.Connected](16 seconds)
    connected.conn must not be (null)
  }
  
  
  it should "disconnect a client" in { f =>
    // TODO https://github.com/angiolep/akka-wamp/issues/29
    pending
  }

  
  case class FixtureParam(router: TestActorRef[Router])
  
  def withFixture(test: OneArgTest) = {
    val router = TestActorRef[Router](Router.props())
    val theFixture = FixtureParam(router)
    try {
      withFixture(test.toNoArgTest(theFixture)) 
    }
    finally {
      system.stop(router)
    }
  }
}
