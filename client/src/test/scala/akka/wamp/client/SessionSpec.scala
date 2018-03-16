package akka.wamp.client

import akka.wamp.router.Router.SimulateShutdown
import org.scalamock.scalatest.MockFactory

class SessionSpec extends DefaultSpec with MockFactory {

  "A client.Session" should "close and reply GOODBYE upon receiving GOODBYE from router" in { f =>
    pending
    f.withSession { session =>
      // TODO https://github.com/angiolep/akka-wamp/issues/11
      f.router ! SimulateShutdown
      // f.listener.expectMsg(SimulateShutdown)
    }
  } 
  
  
  it should "succeed close by sending GOODBYE and expecting to receive GOODBYE in response" in { f =>
    f.withSession { session =>
      whenReady(session.close()) { _ =>
        // TODO improve this test assertions!
        assert(true)
      }
    }
  }

}
