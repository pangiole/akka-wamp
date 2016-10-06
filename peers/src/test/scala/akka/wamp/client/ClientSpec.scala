package akka.wamp.client

import org.scalatest.concurrent.ScalaFutures

class ClientSpec extends ClientFixtureSpec with ScalaFutures {

  "A client" should "fail to establish a connection when invalid uri is given" in { f =>
    val conn = f.client.connect("ws!127.0.0.1:9999/invalid")
    whenReady(conn.failed) { e =>
      e mustBe a [ConnectionException]
      e.getMessage.lines.next mustBe "Illegal URI reference: Invalid input ':', expected 'EOI', '#', '?', !':' or slashSegments (line 1, column 13): ws!127.0.0.1:9999/invalid"
    } 
  }

  it should "fail to establish a connection when unknown subprotocol is given" in { f =>
    val conn = f.client.connect(f.url, subprotocol = "wamp.2.unknown")
    whenReady(conn.failed) { e =>
      e mustBe a[ConnectionException]
      e.getMessage mustBe "wamp.2.unknown is not supported"
    }
  }
  
  it should "fail to establish a connection when router does not accept connection requests" in { f =>
    val conn = f.client.connect("ws://127.0.0.1:9999/unresponsive")
    whenReady(conn.failed) { e =>
      e mustBe a[ConnectionException]
      e.getMessage mustBe "Tcp command [Connect(127.0.0.1:9999,None,List(),Some(10 seconds),true)] failed"
    }
  }

  it should "succeed establishing one or more connections in optimal scenarios" in { f =>
    // 1st connection
    whenReady(f.client.connect(f.url))(_ => assert(true))
    // 2nd connection
    whenReady(f.client.connect(f.url))(_ => assert(true))
    // 3rd connection
    whenReady(f.client.connect(f.url))(_ => assert(true))
  }
  
  
  it should "succeed terminate" in { f =>
    whenReady(f.client.terminate()) { terminated =>
      assert(terminated.existenceConfirmed)
    }
  }
}
