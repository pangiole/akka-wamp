package akka.wamp

import akka.actor._
import akka.io._
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import akka.wamp.messages._
import akka.wamp.router.Router
import org.scalatest._

import scala.concurrent.duration._


// reference.conf is overriding akka.wamp.router.port to enable dynamic port bindings
class ManagerSpec 
  extends TestKit(ActorSystem("test"))
    with FlatSpecLike
    with ImplicitSender
    with MustMatchers
    with BeforeAndAfterAll
{
  // NOTE: this tests suite makes use of a shared fixture, 
  //       implemented as mutable state, which is left over
  //       by each test method to the next one.
  //       Therefore, you cannot mixin ParallelTestExecution!

  var manager: ActorRef = _
  var router: ActorRef = _
  var listener: ActorRef = _
  var handler: ActorRef = _
  var url: String = _
  
  override protected def beforeAll(): Unit = {
    manager = IO(Wamp)
    router = system.actorOf(Router.props())
  }

  "The IO(Wamp) manager" should "bind a router to the default transport" in {
    manager ! Bind(router, transport = "default")
    val bound = expectMsgType[Bound](32 seconds)
    listener = bound.listener
    listener must not be (null)
    // TODO listener mustBe childOf(manager)
    url = bound.url
    url must startWith("ws://127.0.0.1:")
    url must endWith("/ws")
  }

  
  it should "connect a client" in {
    manager ! Connect(url, "wamp.2.json")
    val connected = expectMsgType[Connected](32 seconds)
    handler = connected.inletHandler 
    handler must not be (null)
    // TODO handler mustBe childOf(manager)
  }
  
  
  it should "disconnect a client" in {
    pending
    // TODO it requires WebSocket connection
    val watcher = TestProbe()
    watcher.watch(handler)
    handler ! Disconnect
    expectMsgType[Disconnected]
    val terminated = watcher.expectMsgType[Terminated]
    terminated.actor mustBe handler
  }

  
  it should "unbind a transport listener" in {
    val watcher = TestProbe()
    watcher.watch(listener)
    listener ! Unbind
    expectNoMsg()
    // TODO expectMsgType[Unbound]
    val terminated = watcher.expectMsgType[Terminated]
    terminated.actor mustBe listener
  }

  
  override def afterAll(): Unit = {
    system.stop(router)
    super.afterAll()
  }
  
}
