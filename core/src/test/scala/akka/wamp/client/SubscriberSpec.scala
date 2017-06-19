/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.md', which is part of this source code package.
 */

package akka.wamp.client

import akka.wamp.messages.Event
import org.scalamock.scalatest.MockFactory

import scala.concurrent.duration._



class SubscriberSpec extends ClientBaseSpec with MockFactory {

  "A client.Subscriber" should "fail to subscribe when session is closed" in { f =>
    f.withSession { session =>
      whenReady(session.close()) { _ =>
        val subscription = session.subscribe("mytopic", (_:Event) => ())
        whenReady(subscription.failed) { ex =>
          ex mustBe a[ClientException]
          ex.getMessage mustBe "Session closed"
        }
      }
    }
  }


  it should "succeed to subscribe an event consumer to a topic" in { f =>
    f.withSession { implicit session1 =>
      val consumer = stubFunction[Event, Unit]
      val subscription = session1.subscribe("mytopic", consumer)
      whenReady(subscription) { subscription =>
        subscription.topic mustBe "mytopic"
        subscription.ack.requestId mustBe 1
        subscription.ack.subscriptionId mustBe 1

        // the publisher shall be another transport actor,
        // otherwise the router wouldn't publish the event
        f.withConnection { conn =>
          whenReady(conn.open()) { session2 =>
            val publication = session2.publishAck("mytopic"/*, data = List("paolo", 40, true)*/)
            whenReady(publication) { _ =>
              awaitAssert(consumer.verify(*).once(), 32 seconds)
            }
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
              awaitAssert(consumer.verify().once(), 32 seconds)
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
              awaitAssert(consumer.verify(40).once(), 32 seconds)
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
              awaitAssert(consumer.verify("paolo", 40).once(), 32 seconds)
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
              awaitAssert(consumer.verify("paolo", 40, true).once(), 32 seconds)
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
              awaitAssert(consumer.verify("paolo", 40, true, 61.25).once(), 32 seconds)
            }
          }
        }
      }
    }
  }

  // TODO test macro: subscribe("mytopic", (event: Event) => ())


  it should "succeed unsubscribe from topic" in { f =>
    f.withSession { session =>
      val consumer = stubFunction[Event, Unit]
      val subscription = session.subscribe("mytopic", consumer)
      whenReady(subscription) { subscription =>
        whenReady(subscription.unsubscribe()) { unsubscribed =>
          assert(true)
        }
      }
    }
  }


  it should "fail unsubscribe from topic when session closed" in { f =>
    f.withSession { session =>
      val subscription = session.subscribe("mytopic", (event: Event) => ())
      whenReady(subscription) { subscription =>
        whenReady(session.close()) { _ =>
          val unsubscribed = subscription.unsubscribe()
          whenReady(unsubscribed.failed) { ex =>
            ex mustBe a[ClientException]
            ex.getMessage mustBe "Session closed"
          }
        }
      }
    }
  }
}
