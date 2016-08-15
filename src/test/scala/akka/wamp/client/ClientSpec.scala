package akka.wamp.client

import akka.wamp.messages.ConnectionException

class ClientSpec extends ClientFixtureSpec {

  "The client" should "connect a transport to router" in { f =>
    val client = Client()
    val goodUrl = f.url
    whenReady(client.connect(goodUrl)) { transport =>
      assert(true)
    }
  }

  it should "reject connection when illegal url" in { f =>
    val client = Client()
    val url = "ws!127.0.0.1:9999/illegal"
    client.connect(url).failed.futureValue  match {
      case ConnectionException(message) =>
        message.lines.next mustBe "ConnectionFailed(akka.http.scaladsl.model.IllegalUriException: Illegal URI reference: Invalid input ':', expected 'EOI', '#', '?', !':' or slashSegments (line 1, column 13): ws!127.0.0.1:9999/illegal"
      case other =>
        fail(s"unexpected $other")
    }
  }

  it should "reject connection when illegal subprotocol" in { f =>
    val client = Client()
    val subprotocol = "wamp.2.illegal"
    client.connect(f.url, subprotocol).failed.futureValue  match {
      case ConnectionException(message) =>
        message mustBe "ConnectionFailed(java.util.NoSuchElementException: key not found: wamp.2.illegal)"
      case other =>
        fail(s"unexpected $other")
    }
  }
  
  it should "fail connection when router unresponsive" in { f =>
    val client = Client()
    val url = "ws://127.0.0.1:9999/unresponsive"
    client.connect(url).failed.futureValue match {
      case ConnectionException(message) =>
        message mustBe "ConnectionFailed(java.lang.RuntimeException: Connection failed.)"
      case other =>
        fail(s"unexpected $other")
    }
  }
}
