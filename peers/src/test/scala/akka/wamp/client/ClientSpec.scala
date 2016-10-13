package akka.wamp.client


class ClientSpec extends ClientBaseSpec {

  "A client" should "fail to connect a transport to a malformed URL" in { f =>
    val transport = f.client.connect("ws!malformed:9999/uri")
    whenReady(transport.failed) { cause =>
      cause mustBe a [TransportException]
      cause.getMessage.lines.next mustBe "Illegal URI reference: Invalid input ':', expected 'EOI', '#', '?', !':' or slashSegments (line 1, column 13): ws!malformed:9999/uri"
    } 
  }

  
  it should "fail to connect a transport when unknown subprotocol is given" in { f =>
    val transport = f.client.connect(f.url, subprotocol = "wamp.2.unknown")
    whenReady(transport.failed) { cause =>
      cause mustBe a[TransportException]
      cause.getMessage mustBe "WebSocket upgrade did not finish because of 'unexpected status code: 400 Bad Request'"
    }
  }
  
  
  it should "fail to connect a transport when router host:port is wrong" in { f =>
    val transport = f.client.connect("ws://127.0.0.1:9999/wrong/port")
    whenReady(transport.failed) { cause =>
      cause mustBe a[TransportException]
      cause.getMessage mustBe "Tcp command [Connect(127.0.0.1:9999,None,List(),Some(10 seconds),true)] failed"
    }
  }


  it should "fail to connect a transport when router path is wrong" in { f =>
    val transport = f.client.connect(s"${f.url}/wrong/path")
    whenReady(transport.failed) { cause =>
      cause mustBe a[TransportException]
      cause.getMessage mustBe "WebSocket upgrade did not finish because of 'unexpected status code: 404 Not Found'"
    }
  }

  
  it should "fail to connect a transport after a given number of attempts" in { f =>
    pending
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
