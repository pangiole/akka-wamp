package akka.wamp.client

import akka.wamp.messages.Invocation
import akka.wamp.serialization.Payload
import org.scalamock.scalatest.MockFactory

import scala.concurrent.duration._
import scala.concurrent._

class CallerSpec extends ClientBaseSpec with MockFactory {


  "A caller" should "fail calling procedures when transport is disconnected" in { f =>
    pending
  }
  
  it should "fail calling procedures when session is closed" in { f =>
    f.withSession { session =>
      whenReady(session.close()) { _ =>
        val result = session.call("myapp.procedure")
        whenReady(result.failed) { ex =>
          ex mustBe a[SessionException]
          ex.getMessage mustBe "session closed"
        }
      }
    }
  }

  
  it should "handle error when calling procedures not registered yet" in { f =>
    f.withSession { session =>
      val result = session.call("myapp.unknown.procedure")
      whenReady(result.failed) { cause =>
        cause mustBe a[SessionException]
        cause.getMessage mustBe "wamp.error.no_such_procedure"
      }
    }
  }
  

  it should "succeed call procedure and handle result" in { f =>
    f.withSession { session1 =>
      val handler = stubFunction[Invocation, Future[Payload]]
      val payload = Future.successful(Payload(List("paolo", 40, true)))
      handler.when(*).returns(payload)
      val registration = session1.register("myapp.procedure", handler)
      whenReady(registration) { registration =>
        registration.registered.requestId mustBe 1
        registration.registered.registrationId mustBe 1

        // TODO can a caller invoke itself procedures?

        // the caller shall be another transport actor,
        // otherwise the router wouldn't invoke the procedure
        f.withTransport { transport2 =>
          whenReady(transport2.openSession()) { session2 =>
            val result = session2.call("myapp.procedure")
            whenReady(result) { _ =>
              awaitAssert(handler.verify(*).once().returning(payload), 32 seconds)
            }
          }
        }
      }
    }
  }
  
}
