package akka.wamp

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.{Message => WebSocketMessage}
import akka.http.scaladsl.server.{ExpectedWebSocketRequestRejection, Route, UnsupportedWebSocketSubprotocolRejection}
import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe}
import akka.stream.scaladsl.Flow
import akka.testkit.TestActorRef
import org.scalatest.{MustMatchers, WordSpec}


class RequestHandlerSpec extends WordSpec with MustMatchers with ScalatestRouteTest {
  
  "The websocket handler" should {
    
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

    
    "reject non-websocket requests" in {
      Get(Url) ~> handler.route ~> check {
        rejection mustBe ExpectedWebSocketRequestRejection
      }
      Get(Url) ~> Route.seal(handler.route) ~> check {
        status mustBe StatusCodes.BadRequest
        responseAs[String] mustBe "Expected WebSocket Upgrade request"
      }
    }
    
    
    "handle HELLO message" in {
      val wsClient = WSProbe()

      WS(Url, wsClient.flow, List("other", "wamp.2.json")) ~> handler.route ~> check {
        expectWebSocketUpgradeWithProtocol { protocol =>
          protocol mustBe "wamp.2.json"
          
          wsClient.sendMessage("""[1,"test.realm.uri",{"roles":{"subscriber":{}}}]""")
          wsClient.expectMessage("""[2,1,{"roles":{"broker":{}}}]""")
          //val msg = wsClient.expectMessage()

          //wsClient.sendMessage(BinaryMessage(ByteString("abcdef")))
          // wsClient.expectNoMessage() // will be checked implicitly by next expectation

          //wsClient.sendMessage("John")
          //wsClient.expectMessage("Hello John!")

          //wsClient.sendCompletion()
          //TODO wsClient.expectCompletion()
        }
      }
    }
  }
  
  val router = TestActorRef[Router] 
  val handler = new RequestHandler(router)
  val Url = "http://localhost/wamp"
}