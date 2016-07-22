package akka.wamp

import akka.actor._
import akka.io._
import akka.testkit.{TestActorRef, TestProbe}
import akka.wamp.Wamp._
import akka.wamp.router.Router
import org.scalatest.ParallelTestExecution


// reference.conf is overriding akka.wamp.router.port to enable dynamic port bindings
class ManagerSpec 
  extends ActorSpec(ActorSystem("test"))
{

  "The IO(Wamp) manager" should "bind router" in { f =>
      val manager = IO(Wamp)
      manager ! Bind(f.router)
      f.probe.expectMsgType[Bound]
  }
  
  
  it should "connect client" in { f =>
    val manager = IO(Wamp)
    manager ! Bind(f.router)
    val bound = f.probe.expectMsgType[Bound]
    val address = s"${bound.localAddress.getHostString}:${bound.localAddress.getPort}"

    // connect the router
    manager ! Connect(client = testActor, url = s"ws://$address/ws")
    val connected = expectMsgType[Wamp.Connected]
    connected.peer must not be (null)
  }
  
  
  it should "unbind router" in { f =>
    pending
  }

  
  it should "disconnect client" in { f =>
    pending
  }

  
  
  // see http://www.scalatest.org/user_guide/sharing_fixtures#withFixtureNoArgTest
  /*override def withFixture(test: NoArgTest) = {
    // Perform setup
    try super.withFixture(test) // Invoke the test function
    finally {
      // Perform cleanup
    }
  }*/

  
  // see http://www.scalatest.org/user_guide/sharing_fixtures#loanFixtureMethods
  /*def withProbedRouter(testCode: (TestActorRef[Router], TestProbe) => Any) = {
    val probe = TestProbe("listener")
    val router = TestActorRef[Router](Router.props(listener = Some(probe.ref)))
    testCode(router, probe)
    system.stop(router)
  }*/
  
  
  // see http://www.scalatest.org/user_guide/sharing_fixtures#withFixtureOneArgTest
  case class FixtureParam(router: TestActorRef[Router], probe: TestProbe)
  def withFixture(test: OneArgTest) = {
    val probe = TestProbe()
    val router = TestActorRef[Router](Router.props(listener = Some(probe.ref)))
    val theFixture = FixtureParam(router, probe)
    try {
      withFixture(test.toNoArgTest(theFixture)) // "loan" the fixture to the test
    }
    finally {
      system.stop(probe.ref)
      system.stop(router)
    }
  }
}
