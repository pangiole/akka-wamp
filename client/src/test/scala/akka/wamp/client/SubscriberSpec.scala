/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.md', which is part of this source code package.
 */

package akka.wamp.client

import akka.wamp.messages.Event
import org.scalamock.scalatest.MockFactory

import scala.concurrent.duration._



class SubscriberSpec extends DefaultSpec with MockFactory {

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
              awaitAssert(consumer.verify(*).once(), 8.seconds)
            }
          }
        }
      }
    }
  }


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
