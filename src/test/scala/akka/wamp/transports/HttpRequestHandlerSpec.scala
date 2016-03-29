package akka.wamp.transports

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.{Message => WebSocketMessage}
import akka.http.scaladsl.server.{ExpectedWebSocketRequestRejection, Route, UnsupportedWebSocketSubprotocolRejection}
import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe}
import akka.stream.scaladsl.Flow
import akka.testkit.TestActorRef
import akka.wamp.Router
import org.scalatest.{MustMatchers, WordSpec}


class HttpRequestHandlerSpec extends WordSpec with MustMatchers with ScalatestRouteTest {
  
  "The HTTP request handler" when {
    "connection not yet upgraded" should {

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
  
  "connection upgraded to websocket" should {
    
    "handle ProtocolError('Bad message')" in {
      pending
      checkWith { wsClient =>
        // TODO what shall we expect when client sends bad WAMP messages?
        wsClient.sendMessage("""{bad}""")
      }
    }

    
    "handle ProtocolError('Others')" in {
      pending
    }
    
    
    "handle sessions" in {
      checkWith { wsClient =>
        wsClient.sendMessage("""[1,"akka.wamp.realm",{"roles":{"subscriber":{}}}]""")
        wsClient.expectMessage("""[2,0,{"agent":"akka-wamp-0.1.0","roles":{"broker":{}}}]""")
        //val msg = wsClient.expectMessage()

        //wsClient.sendMessage(BinaryMessage(ByteString("abcdef")))
        // wsClient.expectNoMessage() // will be checked implicitly by next expectation

        //wsClient.sendMessage("John")
        //wsClient.expectMessage("Hello John!")

        //wsClient.sendCompletion()
        //TODO wsClient.expectCompletion()
      }
    }
    
    "handle GOODBYE message" in {
      pending
    }
  }
  
  
  def fakegen(m: Map[Long, _]) = 0L
  val router = TestActorRef(Router.props(fakegen)) 
  val handler = new HttpRequestHandler(router)
  val Url = "http://localhost/wamp"
  
  def checkWith(fn: (WSProbe) => Unit) = {
    val wsClient = WSProbe()
    WS(Url, wsClient.flow, List("wamp.2.json")) ~> handler.route ~> check {
      fn(wsClient)
    }
  }
}