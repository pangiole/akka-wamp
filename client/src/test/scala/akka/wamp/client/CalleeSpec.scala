/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.md', which is part of this source code package.
 */

package akka.wamp.client

import akka.wamp.messages.Invocation
import akka.wamp.serialization.Payload
import org.scalamock.scalatest.MockFactory

import scala.concurrent.Future
import scala.concurrent.duration._


class CalleeSpec extends DefaultSpec with MockFactory {

  "A client.Callee" should "fail to register procedure when session closed" in { f =>
    f.withSession { session =>
      whenReady(session.close()) { _ =>
        val handler = stubFunction[Invocation, Future[Payload]]
        val registration = session.register("myproc", handler)
        whenReady(registration.failed) { ex =>
          ex mustBe a[ClientException]
          ex.getMessage mustBe "Session closed"
        }
      }
    }
  }


  it should "succeed to register an invocation handler as procedure" in { f =>
    f.withSession { ssn1 =>
      val handler = stubFunction[Invocation, Future[Payload]]
      val payload = Future.successful(Payload())
      handler.when(*).returns(payload)
      val registration = ssn1.register("myproc", handler)
      whenReady(registration) { rg =>
        rg.procedure mustBe "myproc"
        rg.ack.requestId mustBe 1
        rg.ack.registrationId mustBe 1
        f.withSession { ssn2 =>
          val result = ssn2.call("myproc")
          whenReady(result) { _ =>
            awaitAssert(handler.verify(*).once().returning(payload), 8.seconds)
          }
        }
      }
    }
  }


  it should "fail to unregister procedure when session closed" in { f =>
    f.withSession { ssn =>
      val handler = stubFunction[Invocation, Future[Payload]]
      val registration = ssn.register("myproc", handler)
      whenReady(registration) { rg =>
        whenReady(ssn.close()) { _ =>
          val unregistered = rg.unregister()
          whenReady(unregistered.failed) { ex =>
            ex mustBe a[ClientException]
            ex.getMessage mustBe "Session closed"
          }
        }
      }
    }
  }


  it should "succeed to unregister procedure" in { f =>
    f.withSession { ssn =>
      val handler = stubFunction[Invocation, Future[Payload]]
      val registration = ssn.register("myproc", handler)
      whenReady(registration) { rg =>
        val unregistered = rg.unregister()
        whenReady(unregistered) { _ =>
          assert(true)
        }
      }
    }
  }
}
