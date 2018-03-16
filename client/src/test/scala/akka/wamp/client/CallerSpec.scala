/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.md', which is part of this source code package.
 */

package akka.wamp.client

import akka.wamp.messages._
import akka.wamp.serialization._
import org.scalamock.scalatest.MockFactory

import scala.concurrent._
import scala.concurrent.duration._


class CallerSpec extends DefaultSpec with MockFactory {

  "A client.Caller" should "fail to call procedures when session is closed" in { f =>
    f.withSession { session =>
      whenReady(session.close()) { _ =>
        val result = session.call("myproc")
        whenReady(result.failed) { ex =>
          ex mustBe a[ClientException]
          ex.getMessage mustBe "Session closed"
        }
      }
    }
  }


  it should "fail to call unknown procedures " in { f =>
    f.withSession { session =>
      val result = session.call("unknown")
      whenReady(result.failed) { cause =>
        cause mustBe a[ClientException]
        cause.getMessage mustBe "wamp.error.no_such_procedure"
      }
    }
  }


  it should "succeed to call procedure and handle result" in { f =>
    f.withSession { session1 =>
      val handler = stubFunction[Invocation, Future[Payload]]
      val payload = Future.successful(Payload(List("paolo", 40, true)))
      handler.when(*).returns(payload)
      val registration = session1.register("myproc", handler)
      whenReady(registration) { registration =>
        registration.ack.requestId mustBe 1
        registration.ack.registrationId mustBe 1

        // TODO can a caller invoke itself procedures?
        f.withSession { session2 =>
          val result = session2.call("myproc")
          whenReady(result) { _ =>
            awaitAssert(handler.verify(*).once().returning(payload), 8.seconds)
          }
        }
      }
    }
  }

  // TODO add more call scenarios (those which YELDs and those which ERRORs)
}
