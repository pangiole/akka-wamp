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


class CalleeSpec extends ClientBaseSpec with MockFactory {

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
            awaitAssert(handler.verify(*).once().returning(payload), 32 seconds)
          }
        }
      }
    }
  }


  it should "succeed to register a 0-parameters lambda handler as procedure" in { f =>
    f.withSession { implicit ssn1 =>
      val registration = register("myproc", () => "done")
      whenReady(registration) { rg =>
        rg.procedure mustBe "myproc"
        rg.ack.requestId mustBe 1
        rg.ack.registrationId mustBe 1

        f.withSession { ssn2 =>
          val result = ssn2.call("myproc")
          whenReady(result) { rs =>
            rs.args(0) mustBe "done"
          }
        }
      }
    }
  }


  it should "succeed to register a 1-parameters lambda handler as procedure" in { f =>
    f.withSession { implicit ssn1 =>
      val registration = register("myproc", (name: String) => name)
      whenReady(registration) { rg =>
        rg.procedure mustBe "myproc"
        rg.ack.requestId mustBe 1
        rg.ack.registrationId mustBe 1

        f.withSession { ssn2 =>
          val result = ssn2.call("myproc", List("paolo"))
          whenReady(result) { rs =>
            rs.args(0) mustBe "paolo"
          }
        }
      }
    }
  }


  it should "succeed to register a 2-parameters lambda handler as procedure" in { f =>
    f.withSession { implicit ssn1 =>
      val registration = register("myproc", (name: String, age: Int) => name.length + age)
      whenReady(registration) { rg =>
        rg.procedure mustBe "myproc"
        rg.ack.requestId mustBe 1
        rg.ack.registrationId mustBe 1

        f.withSession { ssn2 =>
          val result = ssn2.call("myproc", List("paolo", 99))
          whenReady(result) { rs =>
            rs.args(0) mustBe 104
          }
        }
      }
    }
  }


  it should "succeed to register a 3-parameters lambda handler as procedure" in { f =>
    f.withSession { implicit ssn1 =>
      val registration = register("myproc", (name: String, age: Int, male: Boolean) => if (male) 0 else name.length + age)
      whenReady(registration) { rg =>
        rg.procedure mustBe "myproc"
        rg.ack.requestId mustBe 1
        rg.ack.registrationId mustBe 1

        f.withSession { ssn2 =>
          val result = ssn2.call("myproc", List("paolo", 99, true))
          whenReady(result) { rs =>
            rs.args(0) mustBe 0
          }
        }
      }
    }
  }


  it should "succeed to register a 4-parameters lambda handler as procedure" in { f =>
    f.withSession { implicit ssn1 =>
      val registration = register("myproc", (name: String, age: Int, male: Boolean, weight: Double) => (if (male) 0 else name.length + age) * weight)
      whenReady(registration) { rg =>
        rg.procedure mustBe "myproc"
        rg.ack.requestId mustBe 1
        rg.ack.registrationId mustBe 1

        f.withSession { ssn2 =>
          val result = ssn2.call("myproc", List("liliana", 93, false, 0.5))
          whenReady(result) { rs =>
            rs.args(0) mustBe 50
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
