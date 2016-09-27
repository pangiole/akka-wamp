package akka.wamp.client

import akka.wamp.messages.Invocation
import akka.wamp.serialization.Payload
import org.scalamock.scalatest.MockFactory

import scala.concurrent._

class CalleeSpec extends ClientFixtureSpec with MockFactory {


  "A callee" should "fail register procedure when session closed" in { f =>
    f.withSession { session =>
      whenReady(session.close()) { _ =>
        val registration = session.register("myapp.topic")(_ => Future.successful(None))
        whenReady(registration.failed) { ex =>
          ex mustBe a[SessionException]
          ex.getMessage mustBe "session closed"
        }
      }
    }
  }
  


  it should "succeed register procedure" in { f =>
    f.withSession { session1 =>
      val handler = stubFunction[Invocation, Future[Option[Payload]]]
      val registration = session1.register("myapp.procedure")(handler)
      whenReady(registration) { registration =>
        registration.procedure mustBe "myapp.procedure"
        registration.registered.requestId mustBe 1
        registration.registered.registrationId mustBe 1
      }
    }
  }


  it should "fail unregister procedure when session closed" in { f =>
    f.withSession { session =>
      whenReady(session.close()) { _ =>
        val unregistered = session.unregister("myapp.procedure")
        whenReady(unregistered.failed) { ex =>
          ex mustBe a[SessionException]
          ex.getMessage mustBe "session closed"
        }
      }
    }
  }


  it should "succeed unregister procedure" in { f =>
    pending
  }
}
