package akka.wamp

import java.net.URI

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
  var uri: URI = _
  
  override protected def beforeAll(): Unit = {
    manager = IO(Wamp)
    router = system.actorOf(Router.props())
  }

  "The IO(Wamp) manager" should "bind a router to the default transport" in {
    manager ! Bind(router, endpoint = "local")
    val bound = expectMsgType[Bound](32 seconds)
    listener = bound.listener
    listener must not be (null)
    // TODO listener mustBe childOf(manager)
    uri = bound.uri
    uri.getPort must be > 0
  }

  
  it should "connect a client" in {
    manager ! Connect(uri, "json")
    val connected = expectMsgType[Connected](32 seconds)
    handler = connected.handler
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

  
  it should "unbind a connection listener" in {
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
