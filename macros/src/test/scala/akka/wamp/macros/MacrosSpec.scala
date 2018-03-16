package akka.wamp.macros

import org.scalamock.scalatest.MockFactory
import scala.concurrent.duration._


class MacrosSpec extends ActorSpec with MockFactory {

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




  it should "succeed to subscribe a 0-parameters lambda consumer to a topic" in { f =>
    f.withSession { implicit session1 =>
      val consumer = stubFunction[Unit]
      val subscription = subscribe("mytopic", () => { consumer.apply(); () })
      whenReady(subscription) { subscription =>
        f.withConnection { conn =>
          whenReady(conn.open()) { session2 =>
            val publication = session2.publishAck("mytopic", List(40))
            whenReady(publication) { _ =>
              awaitAssert(consumer.verify().once(), 8.seconds)
            }
          }
        }
      }
    }
  }


  it should "succeed to subscribe a 1-parameters lambda consumer to a topic" in { f =>
    f.withSession { implicit session1 =>
      val consumer = stubFunction[Int, Unit]
      val subscription = subscribe("mytopic", (n: Int) => { consumer.apply(n); () })
      whenReady(subscription) { subscription =>
        f.withConnection { conn =>
          whenReady(conn.open()) { session2 =>
            val publication = session2.publishAck("mytopic", List(40))
            whenReady(publication) { _ =>
              awaitAssert(consumer.verify(40).once(), 8.seconds)
            }
          }
        }
      }
    }
  }


  it should "succeed to subscribe a 2-parameters lambda consumer to a topic" in { f =>
    f.withSession { implicit session1 =>
      val consumer = stubFunction[String, Int, Unit]
      val subscription = subscribe("mytopic", (s: String, n: Int) => { consumer.apply(s, n); () })
      whenReady(subscription) { subscription =>
        f.withConnection { conn =>
          whenReady(conn.open()) { session2 =>
            val publication = session2.publishAck("mytopic", List("paolo", 40))
            whenReady(publication) { _ =>
              awaitAssert(consumer.verify("paolo", 40).once(), 8.seconds)
            }
          }
        }
      }
    }
  }


  it should "succeed to subscribe a 3-parameters lambda consumer to a topic" in { f =>
    f.withSession { implicit session1 =>
      val consumer = stubFunction[String, Int, Boolean, Unit]
      val subscription = subscribe("mytopic", (s: String, n: Int, b: Boolean) => { consumer.apply(s, n, b); () })
      whenReady(subscription) { subscription =>
        f.withConnection { conn =>
          whenReady(conn.open()) { session2 =>
            val publication = session2.publishAck("mytopic", List("paolo", 40, true))
            whenReady(publication) { _ =>
              awaitAssert(consumer.verify("paolo", 40, true).once(), 8.seconds)
            }
          }
        }
      }
    }
  }


  it should "succeed to subscribe a 4-parameters lambda consumer to a topic" in { f =>
    f.withSession { implicit session1 =>
      val consumer = stubFunction[String, Int, Boolean, Double, Unit]
      val subscription = subscribe("mytopic", (s: String, n: Int, b: Boolean, d: Double) => { consumer.apply(s, n, b, d); () })
      whenReady(subscription) { subscription =>
        f.withConnection { conn =>
          whenReady(conn.open()) { session2 =>
            val publication = session2.publishAck("mytopic", List("paolo", 40, true, 61.25))
            whenReady(publication) { _ =>
              awaitAssert(consumer.verify("paolo", 40, true, 61.25).once(), 8.seconds)
            }
          }
        }
      }
    }
  }

  // TODO test macro: subscribe("mytopic", (event: Event) => ())

}
