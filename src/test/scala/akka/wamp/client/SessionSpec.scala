package akka.wamp.client

import akka.Done
import akka.wamp.messages.Event
import org.scalamock.scalatest.MockFactory
import scala.concurrent.duration._

class SessionSpec extends ClientFixtureSpec with MockFactory {

  "A client session" should "reply goodbye and close on goodbye from router" in { f =>
    pending
    val session = for (s <- Client().connectAndOpen(f.url)) yield s
    whenReady(session) { session =>
      // TODO cannot simulate Goodbye("wamp.error.system_shutdown") from router
    }
  }
  
  it should "fail publish to topic when it turns to be closed" in { f =>
    pending 
    val session = for (s <- Client().connectAndOpen(f.url)) yield s
    whenReady(session) { session =>
      // TODO cannot simulate publish after session closed
    }
  }

  it should "succeed publish(noack) to topic" in { f =>
    val publication = for {
      session <- Client().connectAndOpen(f.url)
      publication <- session.publish("myapp.topic")
    } yield publication

    whenReady(publication) { publication =>
      import org.scalatest.EitherValues._
      publication.left.value mustBe Done
    }
  }

  it should "succeed publish(ack) to topic and receive ack" in { f =>
    val publication = for {
      session <- Client().connectAndOpen(f.url)
      publication <- session.publish("myapp.topic", acknowledge = true)
    } yield publication

    whenReady(publication) { publication =>
      import org.scalatest.EitherValues._
      publication.right.value must have (
        'requestId(1),
        'publicationId(2)
      )
    }
  }

  it should "succeed subscribe to topic and receive events" in { f =>
    val eventHandler = stubFunction[Event, Unit]
    val publisher = for (session1 <- Client().connectAndOpen(f.url)) yield session1
    whenReady(publisher) { publisher =>
      val subscription = for {
        session2 <- Client().connectAndOpen(f.url)
        subscription <- session2.subscribe("myapp.topic")(eventHandler)
      } yield subscription

      whenReady(subscription) { s =>
        s.requestId mustBe 1
        s.subscriptionId mustBe 1
        val publication = publisher.publish("myapp.topic", acknowledge = true)
        whenReady(publication) { p =>
          awaitAssert(eventHandler.verify(*).once(), 5 seconds)
        }
      }  
    }
  }

  it should "fail subscribe to topic when it turns to be closed" in { f =>
    pending
  }
}
