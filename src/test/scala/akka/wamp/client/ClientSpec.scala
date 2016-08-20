package akka.wamp.client

import org.scalatest.concurrent.ScalaFutures

class ClientSpec extends ClientFixtureSpec  with ScalaFutures {

  "The client" should "fail connect transport when invalid uri is given" in { f =>
    val client = Client()
    val transport = client.connect("ws!127.0.0.1:9999/invalid")
    whenReady(transport.failed) { e =>
      e mustBe a [ConnectionException]
      e.getMessage.lines.next mustBe "ConnectionFailed(akka.http.scaladsl.model.IllegalUriException: Illegal URI reference: Invalid input ':', expected 'EOI', '#', '?', !':' or slashSegments (line 1, column 13): ws!127.0.0.1:9999/invalid"
    }
  }

  it should "fail connect transport when invalid subprotocol is given" in { f =>
    val client = Client()
    val transport = client.connect(f.url, subprotocol = "wamp.2.invalid")
    whenReady(transport.failed) { e =>
      e mustBe a[ConnectionException]
      e.getMessage mustBe "ConnectionFailed(java.util.NoSuchElementException: key not found: wamp.2.invalid)"
    }
  }
  
  it should "fail connect transport when router does not accept connection requests" in { f =>
    val client = Client()
    val transport = client.connect("ws://127.0.0.1:9999/unresponsive")
    whenReady(transport.failed) { e =>
      e mustBe a[ConnectionException]
      e.getMessage mustBe "ConnectionFailed(java.lang.RuntimeException: Connection failed.)"
    }
  }

  it should "succeed connect one or more transports in optimal scenarios" in { f =>
    val client = Client()
    whenReady(client.connect(f.url))(_ => assert(true))
    whenReady(client.connect(f.url))(_ => assert(true))
  }
}
