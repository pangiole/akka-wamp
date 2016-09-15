package akka.wamp.router

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.{Message => WebSocketMessage}
import akka.http.scaladsl.server._
import akka.stream.scaladsl._

/**
  * It tests the ``router.Transport`` behaviour with its 
  * 
  *   DEFAULT configuration
  * 
  * (NO custom settings) by exercising it with various JSON messages covering
  * the most common scenarios (session handling, subscriptions, publications, etc.)
  */
class WsTransportSpec extends WsTransportFixtureSpec
{
  "The default router.Transport" should "reject websocket requests if no subprotocol matches" in { fixture =>
    WS(URL, clientSideHandler = Flow[WebSocketMessage]) ~> fixture.httpRoute ~> check {
      rejections.collect {
        case UnsupportedWebSocketSubprotocolRejection(p) => p
      }.toSet mustBe Set("wamp.2.json")
    }
    WS(URL, clientSideHandler = Flow[WebSocketMessage], List("other")) ~> Route.seal(fixture.httpRoute) ~> check {
      status mustBe StatusCodes.BadRequest
      responseAs[String] mustBe "None of the websocket subprotocols offered in the request are supported. Supported are 'wamp.2.json'."
      header("Sec-WebSocket-Protocol").get.value() mustBe "wamp.2.json"
    }
  }


  it should "reject any non-websocket requests" in { fixture =>
    Get(URL) ~> fixture.httpRoute ~> check {
      rejection mustBe ExpectedWebSocketRequestRejection
    }
    Get(URL) ~> Route.seal(fixture.httpRoute) ~> check {
      status mustBe StatusCodes.BadRequest
      responseAs[String] mustBe "Expected WebSocket Upgrade request"
    }
  }

  it should "handle sessions" in { fixture =>
    withWsClient(fixture.httpRoute) { client =>

      // --> HELLOs
      client.sendMessage("""[1,"invalid..realm",{"roles":{"subscriber":{}}}]""")
      client.sendMessage("""[1,"myapp.realm",{"roles":{"invalid":{}}}]""")
      client.sendMessage("""[1,"myapp.realm",{"roles":{"publisher":{}}}]""")

      // <-- WELCOME
      client.expectMessage("""[2,1,{"agent":"akka-wamp-0.7.0","roles":{"broker":{},"dealer":{}}}]""")

      // SESSION #1 OPEN

      // --> GOODBYEs
      client.sendMessage("""[6,{},"invalid..reason"]""")
      client.sendMessage("""[6,{},"wamp.error.close_realm"]""")

      // <-- GOODBYE
      client.expectMessage("""[6,{},"wamp.error.goodbye_and_out"]""")

      // SESSION #1 CLOSED
      // but TRANSPORT still CONNECTED!

      // --> HELLO
      client.sendMessage("""[1,"myapp.realm",{"roles":{"subscriber":{}}}]""")

      // <-- WELCOME
      client.expectMessage("""[2,2,{"agent":"akka-wamp-0.7.0","roles":{"broker":{},"dealer":{}}}]""")

      // SESSION #2 OPEN

      // --> GOODBYE
      client.sendMessage("""[6,{},"wamp.error.close_realm"]""")

      // <-- GOODBYE
      client.expectMessage("""[6,{},"wamp.error.goodbye_and_out"]""")

      // SESSION #2 OPEN
      // but TRANSPORT still CONNECTED!

      client.expectNoMessage()
    }
  }
  
  
  it should "handle messages exchanged in publish/subscribe scenario" in { fixture =>
    withWsClient(fixture.httpRoute) { client =>
      // --> HELLO 
      client.sendMessage("""[1,"myapp.realm",  {"roles":{"subscriber":{}, "publisher":{}}}]""")
      
      // <-- WELCOME
      client.expectMessage("""[2,1,{"agent":"akka-wamp-0.7.0","roles":{"broker":{},"dealer":{}}}]""")

      // SESSION OPEN
      
      // --> SUBSCRIBEs
      client.sendMessage("""[32,1,{},"invalid..topic'"]""")
      client.sendMessage("""[32,1,{},"myapp.TOPIC-"]""")
      
      // <-- SUBSCRIBED
      client.expectMessage("""[33,1,1]""")
      
      // --> PUBLISHes
      client.sendMessage("""[16,2,{"acknowledge":true},"invalid..topic"]""")
      client.sendMessage("""[16,2,{"acknowledge":true},"myapp.TOPIC-"]""")
      
      // <-- PUBLISHED
      client.expectMessage("""[17,2,2]""")
      client.expectNoMessage()
    }
  }
}