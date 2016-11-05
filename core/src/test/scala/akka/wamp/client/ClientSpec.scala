package akka.wamp.client

import akka.http.scaladsl.model.IllegalUriException
import akka.stream.StreamTcpException
import com.typesafe.config.ConfigException

class ClientSpec extends ClientBaseSpec {

  "A client" should "fail to connect when 'unknown' named transport is given" in { f =>
    pending
    // NOTE: there seems to be a ScalaTest bug here
    val conn = f.client.connect("unknown")
    whenReady(conn.failed) { ex =>
      ex mustBe a[ClientException]
      ex.getCause mustBe a[ConfigException.Missing]
      ex.getMessage mustBe "No configuration setting found for key 'transport.unknown'"
    }
  }


  it should "fail to connect to a malformed URL" in { f =>
    val conn = f.client.connect("ws!malformed:9999/uri", "json")
    whenReady(conn.failed) { ex =>
      ex mustBe a [ClientException]
      ex.getCause mustBe a [IllegalUriException]
      ex.getMessage.lines.next mustBe "Illegal URI reference: Invalid input ':', expected 'EOI', '#', '?', !':' or slashSegments (line 1, column 13): ws!malformed:9999/uri"
    }
  }

  
  it should "fail to connect when 'unknown' format is given" in { f =>
    val conn = f.client.connect(f.url, format = "unknown")
    whenReady(conn.failed) { ex =>
      ex mustBe a[ClientException]
      ex.getCause mustBe a[IllegalArgumentException]
      ex.getMessage mustBe "WebSocket upgrade did not finish because of 'unexpected status code: 400 Bad Request'"
    }
  }

  
  it should "fail to connect when '/wrong/path' is given" in { f =>
    val conn = f.client.connect(s"${f.url}/wrong/path", "json")
    whenReady(conn.failed) { ex =>
      ex mustBe a[ClientException]
      ex.getCause mustBe a[IllegalArgumentException]
      ex.getMessage mustBe "WebSocket upgrade did not finish because of 'unexpected status code: 404 Not Found'"
    }
  }

  
  // TODO it should "repeatedly attempt to connect when router unreachable"
  
  
  it should "succeed establishing one or more connections to the same router" in { f =>
    val conn1 = f.client.connect(f.url, "json")
    whenReady(conn1) { c1 =>
      c1.disconnected mustBe false
      val conn2 = f.client.connect(f.url, "json")
      whenReady(conn2) { c2 =>
        c2 mustNot equal(c1)
        c2.disconnected mustBe false
        val conn3 = f.client.connect(f.url, "json")
        whenReady(conn3) { c3 =>
          c3 mustNot equal(c2)
          c3.disconnected mustBe false
        }
      }
    }
  }


}
