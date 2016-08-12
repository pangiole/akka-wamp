package akka.wamp.client

import akka.wamp.router._
import org.scalatest.ParallelTestExecution
import org.scalatest.concurrent._
import org.scalatest.time.SpanSugar

class ClientSpec extends RouterFixtureSpec 
  with ScalaFutures with SpanSugar with ParallelTestExecution {

  implicit val defaultPatience =
    PatienceConfig(timeout = 6 seconds, interval = 100 millis)
  
  "The client" should "connect when attempts right url" in { f =>
    val client = Client()
    val rightUrl = f.url
    whenReady(client.connect(rightUrl)) { transport =>
      assert(true)
    }
  }
  
  it should "fail when attempts wrong url" in { f =>
    val client = Client()
    val wrongUrl = "ws://127.0.0.1:9999/wrong"
    client.connect(wrongUrl).failed.futureValue mustBe a[ThrownMessage]
  }
  
}
