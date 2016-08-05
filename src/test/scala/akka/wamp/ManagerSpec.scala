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
    manager ! Bind(f.probe.ref)
    f.probe.expectMsgType[Bound](8 seconds)
  }
  
  
  it should "connect client" in { f =>
    val manager = IO(Wamp)
    manager ! Bind(f.router)
    
    val bound = f.probe.expectMsgType[Bound](8 seconds)
    val address = s"${bound.localAddress.getHostString}:${bound.localAddress.getPort}"

    // connect the router
    manager ! Connect(client = testActor, url = s"ws://$address/ws")
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
  case class FixtureParam(router: TestActorRef[Router], probe: TestProbe)
  def withFixture(test: OneArgTest) = {
    val probe = TestProbe()
    val router = TestActorRef[Router](Router.props(probe = Some(probe.ref)))
    val theFixture = FixtureParam(router, probe)
    try {
      withFixture(test.toNoArgTest(theFixture)) 
    }
    finally {
      system.stop(probe.ref)
      system.stop(router)
    }
  }
}
