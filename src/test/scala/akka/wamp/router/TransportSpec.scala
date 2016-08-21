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

/**
  * This tests suite is meant to put the router.Transport under test
  */
class TransportSpec 
  extends fixture.FlatSpec 
    with MustMatchers with BeforeAndAfterAll
    with ScalatestRouteTest 
    with ParallelTestExecution
{
  
  "A router transport" should "reject websocket requests if no subprotocol matches" in { f =>
    WS(url, Flow[WebSocketMessage]) ~> f.httpRoute ~> check {
      rejections.collect {
        case UnsupportedWebSocketSubprotocolRejection(p) => p
      }.toSet mustBe Set("wamp.2.json")
    }
    WS(url, Flow[WebSocketMessage], List("other")) ~> Route.seal(f.httpRoute) ~> check {
      status mustBe StatusCodes.BadRequest
      responseAs[String] mustBe "None of the websocket subprotocols offered in the request are supported. Supported are 'wamp.2.json'."
      header("Sec-WebSocket-Protocol").get.value() mustBe "wamp.2.json"
    }
  }
   
  
  it should "reject any non-websocket requests" in { fixture =>
    Get(url) ~> fixture.httpRoute ~> check {
      rejection mustBe ExpectedWebSocketRequestRejection
    }
    Get(url) ~> Route.seal(fixture.httpRoute) ~> check {
      status mustBe StatusCodes.BadRequest
      responseAs[String] mustBe "Expected WebSocket Upgrade request"
    }
  }


  it should "drop unparsable messages but keep client connected" in { fixture =>
    withHandler(fixture.httpRoute) { client =>
      
      // --> !$@**^$@£
      client.sendMessage("""!$@**^$@£""")
      
      // we expect the above unparsable message to have been dropped
      // and the client still connected to the router for the next message

      // --> HELLO
      client.sendMessage("""[1,"akka.wamp.realm",{"roles":{"subscriber":{}}}]""")

      // <-- WELCOME
      client.expectMessage("""[2,1,{"agent":"akka-wamp-0.5.1","roles":{"broker":{}}}]""")
    }
  }
  
  
  it should "handle messages exchanged in subscription scenario" in { fixture =>
    withHandler(fixture.httpRoute) { client =>
      
      // --> HELLO
      client.sendMessage("""[1,"akka.wamp.realm",{"roles":{"subscriber":{}}}]""")
      
      // <-- WELCOME
      client.expectMessage("""[2,1,{"agent":"akka-wamp-0.5.1","roles":{"broker":{}}}]""")
      
      // --> SUBSCRIBE
      client.sendMessage("""[32,1,{},"com.myapp.mytopic1"]""")
      
      // <-- SUBSCRIBED
      client.expectMessage("""[33,1,1]""")
      
      // --> GOODBYE
      client.sendMessage("""[6,{"message":"The host is shutting down now."},"wamp.error.close_realm"]""")

      // <-- GOODBYE
      client.expectMessage("""[6,{},"wamp.error.goodbye_and_out"]""")
    }
  }

  
  
  def withHandler(route: Route)(testCode: (WSProbe) => Unit) = {
    val client = WSProbe()
    WS(url, client.flow, List("wamp.2.json")) ~> route ~> check {
      testCode(client)
    }
  }

  case class FixtureParam(httpRoute: Route)
  
  override def withFixture(test: OneArgTest) = {
    val scopes = Map(
      'global -> new Scope.Session {},
      'router -> new Scope.Session {},
      'session -> new Scope.Session {}
    )
    val wampRouter = TestActorRef[Router](Router.props(scopes))
    val transport = TestActorRef[Transport](Transport.props(wampRouter))
    val httpRoute: Route = transport.underlyingActor.httpRoute
    
    val theFixture = FixtureParam(httpRoute)
    try {
      IO(Wamp) ! Bind(wampRouter)
      withFixture(test.toNoArgTest(theFixture))
    }
    finally {
      system.stop(wampRouter)
    }
  }

  val url = "http://127.0.0.1:8080/ws"

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }
}