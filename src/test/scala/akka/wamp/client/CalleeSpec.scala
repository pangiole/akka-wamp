package akka.wamp.client

import akka.wamp.messages.{Event, Invocation}
import org.scalamock.scalatest.MockFactory

import scala.concurrent.duration._

class CalleeSpec extends ClientFixtureSpec with MockFactory {


  "A client callee" should "fail register procedure when it turns out to be closed" in { f =>
    f.withSession { session =>
      whenReady(session.close()) { _ =>
        val registration = session.register("myapp.topic"){_ => ()}
        whenReady(registration.failed) { ex =>
          ex mustBe a[SessionException]
          ex.getMessage mustBe "session closed"
        }
      }
    }
  }


  it should "fail register procedure if it didn't open session with 'callee' role" in { f =>
    pending
  }


  it should "succeed register procedure and expect its handler to be passed INVOCATIONs in" in { f =>
    f.withSession { session1 =>
      val handler = stubFunction[Invocation, Unit]
      val registration = session1.register("myapp.procedure")(handler)
      whenReady(registration) { registration =>
        registration.requestId mustBe 1
        registration.registrationId mustBe 1

        pending
        
        // the caller shall be another connection actor,
        // otherwise the router wouldn't call the procedure
        /*f.withConnection { conn2 =>
          whenReady(conn2.openSession()) { session2 =>
            val call = session2.call("myapp.procedure")
            whenReady(call) { _ =>
              awaitAssert(handler.verify(*).once(), 5 seconds)
            }
          }
        }*/
      }
    }
  }
}
