package akka.wamp.router

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.{Message => WebSocketMessage}
import akka.http.scaladsl.server._
import akka.http.scaladsl.testkit._
import akka.io.IO
import akka.stream.scaladsl._
import akka.testkit._
import akka.wamp.Wamp._
import akka.wamp.{Scope, Wamp}
import org.scalatest._

class TransportSpec 
  extends fixture.FlatSpec 
    with MustMatchers with BeforeAndAfterAll
    with ScalatestRouteTest 
    with ParallelTestExecution
{
  
  "The router transport" should "reject websocket requests if no subprotocol matches" in { f =>
    WS(url, Flow[WebSocketMessage]) ~> f.route ~> check {
      rejections.collect {
        case UnsupportedWebSocketSubprotocolRejection(p) => p
      }.toSet mustBe Set("wamp.2.json")
    }
    WS(url, Flow[WebSocketMessage], List("other")) ~> Route.seal(f.route) ~> check {
      status mustBe StatusCodes.BadRequest
      responseAs[String] mustBe "None of the websocket subprotocols offered in the request are supported. Supported are 'wamp.2.json'."
      header("Sec-WebSocket-Protocol").get.value() mustBe "wamp.2.json"
    }
  }
   
  
  it should "reject any non-websocket requests" in { f =>
    Get(url) ~> f.route ~> check {
      rejection mustBe ExpectedWebSocketRequestRejection
    }
    Get(url) ~> Route.seal(f.route) ~> check {
      status mustBe StatusCodes.BadRequest
      responseAs[String] mustBe "Expected WebSocket Upgrade request"
    }
  }
  
  
  it should "handle subscription scenario" in { f =>
    scenario(f.route) { client =>
      
      // >>> HELLO
      client.sendMessage("""[1,"akka.wamp.realm",{"roles":{"subscriber":{}}}]""")
      
      // <<< WELCOME
      client.expectMessage("""[2,1,{"agent":"akka-wamp-0.4.0","roles":{"broker":{}}}]""")
      
      // >>> SUBSCRIBE
      client.sendMessage("""[32,1,{},"com.myapp.mytopic1"]""")
      
      // <<< SUBSCRIBED
      client.expectMessage("""[33,1,1]""")
      
      // >>> GOODBYE
      client.sendMessage("""[6,{"message":"The host is shutting down now."},"wamp.error.close_realm"]""")

      // <<< GOODBYE
      client.expectMessage("""[6,{},"wamp.error.goodbye_and_out"]""")
    }
  }

  
  
  def scenario(route: Route)(testCode: (WSProbe) => Unit) = {
    val client = WSProbe()
    WS(url, client.flow, List("wamp.2.json")) ~> route ~> check {
      testCode(client)
    }
  }

  // see http://www.scalatest.org/user_guide/sharing_fixtures#withFixtureOneArgTest
  case class FixtureParam(route: Route)
  override def withFixture(test: OneArgTest) = {
    val scopes = Map(
      'global -> new Scope.Session {},
      'router -> new Scope.Session {},
      'session -> new Scope.Session {}
    )
    val router = TestActorRef[Router](Router.props(scopes))
    val transport = TestActorRef[Transport](Transport.props(router))
    val route = transport.underlyingActor.httpRoute
    
    val theFixture = FixtureParam(route)
    try {
      IO(Wamp) ! Bind(router)
      withFixture(test.toNoArgTest(theFixture)) // "loan" the fixture to the test
    }
    finally {
      system.stop(router)
    }
  }

  val url = "http://127.0.0.1:8080/ws"

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }
}