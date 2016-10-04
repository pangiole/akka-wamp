package akka.wamp.client

import akka.wamp.messages.Event
import org.scalamock.scalatest.MockFactory
import scala.concurrent.duration._

class SubscriberSpec extends ClientFixtureSpec with MockFactory {

  "A subscriber" should "fail subscribe to topic when session closed" in { f =>
    f.withSession { session =>
      whenReady(session.close()) { _ =>
        val subscription = session.subscribe("myapp.topic"){_ => ()}
        whenReady(subscription.failed) { ex =>
          ex mustBe a[SessionException]
          ex.getMessage mustBe "session closed"
        }
      }
    }
  }


  it should "succeed subscribe to topic and expect events" in { f =>
    f.withSession { session1 =>
      val handler = stubFunction[Event, Unit]
      val subscription = session1.subscribe("myapp.topic")(handler)
      whenReady(subscription) { subscription =>
        subscription.topic mustBe "myapp.topic"
        subscription.subscribed.requestId mustBe 1
        subscription.subscribed.subscriptionId mustBe 1

        // the publisher shall be another connection actor,
        // otherwise the router wouldn't publish the event
        f.withConnection { conn2 =>
          whenReady(conn2.openSession()) { session2 =>
            val publication = session2.publish("myapp.topic", ack=true/*, data = List("paolo", 40, true)*/)
            whenReady(publication) { _ =>
              awaitAssert(handler.verify(*).once(), 5 seconds)
            }
          }
        }
      }
    }
  }


  it should "fail unsubscribe to topic when session closed" in { f =>
    f.withSession { session =>
      whenReady(session.close()) { _ =>
        val unsubscribed = session.unsubscribe("myapp.topic")
        whenReady(unsubscribed.failed) { ex =>
          ex mustBe a[SessionException]
          ex.getMessage mustBe "session closed"
        }
      }
    }
  }


  it should "succeed unsubscribe from topic" in { f =>
    f.withSession { session =>
      val handler = stubFunction[Event, Unit]
      val subscription = session.subscribe("myapp.topic")(handler)
      whenReady(subscription) { subscription =>
        whenReady(subscription.unsubscribe()) { unsubscribed =>
          assert(true)
        }
      }
    }
  }
}
