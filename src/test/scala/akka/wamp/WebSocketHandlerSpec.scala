package akka.wamp

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws.{Message => WebSocketMessage}
import akka.http.scaladsl.server._
import akka.http.scaladsl.testkit._
import akka.stream.scaladsl._
import akka.testkit._
import org.scalatest._


class WebSocketHandlerSpec extends WordSpec with MustMatchers with ScalatestRouteTest {
  
  "The WebSocket handler" when {
    "connection has not upgraded yet" should {

      "reject websocket requests if no subprotocol matches" in {
        WS(Url, Flow[WebSocketMessage]) ~> handler.route ~> check {
          rejections.collect {
            case UnsupportedWebSocketSubprotocolRejection(p) => p
          }.toSet mustBe Set("wamp.2.json")
        }
        WS(Url, Flow[WebSocketMessage], List("other")) ~> Route.seal(handler.route) ~> check {
          status mustBe StatusCodes.BadRequest
          responseAs[String] mustBe "None of the websocket subprotocols offered in the request are supported. Supported are 'wamp.2.json'."
          header("Sec-WebSocket-Protocol").get.value() mustBe "wamp.2.json"
        }
      }
      
      "reject any non-websocket requests" in {
        Get(Url) ~> handler.route ~> check {
          rejection mustBe ExpectedWebSocketRequestRejection
        }
        Get(Url) ~> Route.seal(handler.route) ~> check {
          status mustBe StatusCodes.BadRequest
          responseAs[String] mustBe "Expected WebSocket Upgrade request"
        }
      }
    }
  }
  
  "connection has upgraded to WebSocket protocol" should {
    
    "handle WAMP messages" in {
      checkWith { wsClient =>
        
        // -> HELLO
        wsClient.sendMessage("""[1,"akka.wamp.realm",{"roles":{"subscriber":{}}}]""")
        
        // <- WELCOME
        wsClient.expectMessage("""[2,0,{"agent":"akka-wamp-0.1.0","roles":{"broker":{}}}]""")
        
        // -> SUBSCRIBE
        wsClient.sendMessage("""[32,1,{},"com.myapp.mytopic1"]""")
        
        // <- SUBSCRIBED
        wsClient.expectMessage("""[33,1,0]""")
        
        // -> GOODBYE
        wsClient.sendMessage("""[6,{"message":"The host is shutting down now."},"wamp.error.system_shutdown"]""")

        // -> GOODBYE
        wsClient.expectMessage("""[6,{},"wamp.error.goodbye_and_out"]""")
      }
    }
  }
  
  
  val router = TestActorRef(Router.props(_ + 1)) 
  val handler = new WebSocketHandler(router)
  val Url = "http://localhost/wamp"
  
  def checkWith(fn: (WSProbe) => Unit) = {
    val wsClient = WSProbe()
    WS(Url, wsClient.flow, List("wamp.2.json")) ~> handler.route ~> check {
      fn(wsClient)
    }
  }
}