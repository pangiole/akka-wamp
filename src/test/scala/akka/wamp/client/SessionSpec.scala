package akka.wamp.client

import akka.Done
import akka.wamp.Dict
import akka.wamp.messages.{Event, Goodbye}
import org.scalamock.scalatest.MockFactory

import scala.concurrent.duration._

class SessionSpec extends ClientFixtureSpec with MockFactory {

  "A client session" should "reply GOODBYE upon receiving GOODBYE from router" in { f =>
    // TODO https://github.com/angiolep/akka-wamp/issues/11
    pending
    f.withSession { session =>
      // make the router send Goodbye("wamp.error.system_shutdown")
      // routerManager ! ShutDown
      // f.listener.expectMsg(ShutDown)
      // ???
    }
  } 
  
  
  it should "succeed close by sending GOODBYE and expecting to receive GOODBYE in response" in { f =>
    f.withSession { session =>
      whenReady(session.close()) { goodbyeFromRouter =>
        goodbyeFromRouter.reason mustBe "wamp.error.goodbye_and_out"
        goodbyeFromRouter.details mustBe Dict()
      }
    }
  }
  
  
  it should "fail publish to topic when it turns out to be closed" in { f =>
    f.withSession { session => 
      whenReady(session.close()) { _ =>
        val publication = session.publish("myapp.topic", ack = true)
        whenReady(publication.failed) { ex =>
          ex mustBe a[SessionException]
          ex.getMessage mustBe "session closed"
        } 
      }
    }
  }

  
  it should "succeed publish(noack) to topic" in { f =>
    f.withSession { session =>
      val publication = session.publish("myapp.topic")
      whenReady(publication) { publication =>
        import org.scalatest.EitherValues._
        publication.left.value mustBe Done
      }
    }
  }

  
  it should "succeed publish(ack) to topic and expect to receive PUBLISHED" in { f =>
    f.withSession { session =>
      val publication = session.publish("myapp.topic", ack=true)
      whenReady(publication) { publication =>
        import org.scalatest.EitherValues._
        publication.right.value must have (
          'requestId(1),
          'publicationId(2)
        )
      }
    }
  }

  
  it should "fail subscribe on topic when it turns out to be closed" in { f =>
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

  
  it should "succeed subscribe on topic and expect its handler to be passed EVENTS in" in { f =>
    f.withSession { session1 =>
      val eventHandler = stubFunction[Event, Unit]
      val subscription = session1.subscribe("myapp.topic")(eventHandler)
      whenReady(subscription) { subscription =>
        subscription.requestId mustBe 1
        subscription.subscriptionId mustBe 1
        
        // the publisher shall be another client,
        // otherwise the router wouldn't publish the event
        f.withClient { client2 =>
          whenReady(client2.connect(f.url)) { conn2 =>
            whenReady(conn2.openSession()) { session2 =>
              val publication = session2.publish("myapp.topic", ack=true)
              whenReady(publication) { _ =>
                awaitAssert(eventHandler.verify(*).once(), 5 seconds)
              }
            }
          }
        }
      }
    }
  }
}
