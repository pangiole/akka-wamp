package akka.wamp.client

import akka.Done
import akka.wamp.messages._
import akka.wamp.router.Router.SimulateShutdown
import akka.wamp.serialization.Payload
import org.scalamock.scalatest.MockFactory

import scala.concurrent._
import scala.concurrent.duration._

class SessionSpec extends ClientBaseSpec with MockFactory {

  "A client.Session" should "close and reply GOODBYE upon receiving GOODBYE from router" in { f =>
    // TODO https://github.com/angiolep/akka-wamp/issues/11
    pending
    f.withSession { session =>
      f.router ! SimulateShutdown
      // f.listener.expectMsg(SimulateShutdown)
    }
  } 
  
  
  it should "succeed close by sending GOODBYE and expecting to receive GOODBYE in response" in { f =>
    f.withSession { session =>
      whenReady(session.close()) { _ =>
        assert(true)
      }
    }
  }

}
