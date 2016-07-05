package akka.wamp.router

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws.{Message => WebSocketMessage}
import akka.http.scaladsl.server._
import akka.http.scaladsl.testkit._
import akka.stream.scaladsl._
import akka.testkit._
import org.scalatest._

class TransportSpec extends WordSpec 
  with MustMatchers with ScalatestRouteTest
  with RouterFixture
{
  
  "The WAMP manager" when {
    "connection has not upgraded yet" should {

      "reject websocket requests if no subprotocol matches" in {
        WS(url, Flow[WebSocketMessage]) ~> route ~> check {
          rejections.collect {
            case UnsupportedWebSocketSubprotocolRejection(p) => p
          }.toSet mustBe Set("wamp.2.json")
        }
        WS(url, Flow[WebSocketMessage], List("other")) ~> Route.seal(route) ~> check {
          status mustBe StatusCodes.BadRequest
          responseAs[String] mustBe "None of the websocket subprotocols offered in the request are supported. Supported are 'wamp.2.json'."
          header("Sec-WebSocket-Protocol").get.value() mustBe "wamp.2.json"
        }
      }
      
      "reject any non-websocket requests" in {
        Get(url) ~> route ~> check {
          rejection mustBe ExpectedWebSocketRequestRejection
        }
        Get(url) ~> Route.seal(route) ~> check {
          status mustBe StatusCodes.BadRequest
          responseAs[String] mustBe "Expected WebSocket Upgrade request"
        }
      }
    }
  }
  
  "connection has upgraded to WebSocket protocol" should {
    
    "handle WAMP messages" in {
      scenario { client =>
        
        // -> HELLO
        client.sendMessage("""[1,"akka.wamp.realm",{"roles":{"subscriber":{}}}]""")
        
        // <- WELCOME
        client.expectMessage("""[2,1,{"agent":"akka-wamp-0.2.1","roles":{"broker":{}}}]""")
        
        // -> SUBSCRIBE
        client.sendMessage("""[32,1,{},"com.myapp.mytopic1"]""")
        
        // <- SUBSCRIBED
        client.expectMessage("""[33,1,1]""")
        
        // -> GOODBYE
        client.sendMessage("""[6,{"message":"The host is shutting down now."},"wamp.error.system_shutdown"]""")

        // -> GOODBYE
        client.expectMessage("""[6,{},"wamp.error.goodbye_and_out"]""")
      }
    }
  }

  val router = TestActorRef(Router.props(scopes))
  val transport = TestActorRef[Transport](Transport.props(router))
  val route = transport.underlyingActor.websocketHandler
  val url = "http://localhost:8080/wamp"
  val subprotocols = List("wamp.2.json")
  
  
  def scenario(fn: (WSProbe) => Unit) = {
    val client = WSProbe()
    WS(url, client.flow, subprotocols) ~> route ~> check {
      fn(client)
    }
  }
}