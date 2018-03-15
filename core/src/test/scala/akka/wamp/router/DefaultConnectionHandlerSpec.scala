package akka.wamp.router

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.{Message => WebSocketMessage}
import akka.http.scaladsl.server._
import akka.stream.scaladsl._

/**
  * It tests the ``router.ConnectionHandler`` behaviour with its
  *
  *   DEFAULT configuration
  *
  * (NO custom settings) by exercising it with various JSON messages covering
  * the most common scenarios (session handling, subscriptions, publications, etc.)
  */
class DefaultConnectionHandlerSpec extends ConnectionHandlerBaseSpec
{
  "The default router.ConnectionHandler" should "reject websocket requests if no format matches" in { f =>
    WS(url, clientSideHandler = Flow[WebSocketMessage]) ~> f.httpRoute ~> check {
      rejections.collect {
        case UnsupportedWebSocketSubprotocolRejection(p) => p
      }.toSet mustBe Set("wamp.2.json")
    }
    WS(url, clientSideHandler = Flow[WebSocketMessage], List("other")) ~> Route.seal(f.httpRoute) ~> check {
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


  it should "complete publish/subscribe in optimal scenario" in { f =>
    // --> HELLO
    f.client.sendMessage("""[1,"myrealm",{"roles":{"publisher":{}}}]""")
    // <-- WELCOME
    f.client.expectMessage("""[2,1,{"agent":"akka-wamp-0.15.2","roles":{"broker":{},"dealer":{}}}]""")

    // TODO https://github.com/angiolep/akka-wamp/issues/11
    // TODO how to verify EVENT messages are received?

    // SESSION #1 OPEN
    // \
    // --> SUBSCRIBE(#1)
    f.client.sendMessage("""[32,1,{},"myapp.topic"]""")
    // <-- SUBSCRIBED
    f.client.expectMessage("""[33,1,1]""")

    // --> PUBLISH(#3)
    f.client.sendMessage("""[16,2,{"acknowledge":true},"myapp.topic"]""")
    // <-- PUBLISHED
    f.client.expectMessage("""[17,2,2]""")

    // --> GOODBYE
    f.client.sendMessage("""[6,{},"wamp.error.close_realm"]""")
    // <-- GOODBYE
    f.client.expectMessage("""[6,{},"wamp.error.goodbye_and_out"]""")
    // \
    // SESSION #1 CLOSED

    f.client.expectNoMessage()
  }



  it should "complete remote procedure call in optimal scenario" in { f =>
    // --> HELLO
    f.client.sendMessage("""[1,"myrealm",{"roles":{"publisher":{}}}]""")
    // <-- WELCOME
    f.client.expectMessage("""[2,1,{"agent":"akka-wamp-0.15.2","roles":{"broker":{},"dealer":{}}}]""")

    // SESSION #1 OPEN
    // \
    // --> REGISTER(#1)
    f.client.sendMessage("""[64,1,{},"myapp.procedure.itself"]""")
    // <-- REGISTERED
    f.client.expectMessage("""[65,1,1]""")

    // --> CALL(#2)
    f.client.sendMessage("""[48,2,{},"myapp.procedure.itself",["args0",1],{"key":"value"}]""")
    // <-- INVOCATION(#1)
    f.client.expectMessage("""[68,1,1,{},["args0",1],{"key":"value"}]""")

    // --> YIELD(#1)
    f.client.sendMessage("""[70,1,{},[{"prop","res"}]]""")
    // <-- RESULT(#2)
    f.client.expectMessage("""[50,2,{},[{"prop","res"}]]""")

    // --> GOODBYE
    f.client.sendMessage("""[6,{},"wamp.error.close_realm"]""")
    // <-- GOODBYE
    f.client.expectMessage("""[6,{},"wamp.error.goodbye_and_out"]""")
    // \
    // SESSION #2 CLOSED

    f.client.expectNoMessage()
  }


  it should "disconnect upon offending messages" in { f =>
    pending
  }
}
