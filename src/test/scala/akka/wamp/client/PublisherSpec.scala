package akka.wamp.client

import akka.Done
import org.scalamock.scalatest.MockFactory

class PublisherSpec extends ClientFixtureSpec with MockFactory {

  "A client callee" should "fail publish to topic if it turns out to be closed" in { f =>
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


  it should "fail publish to topic if it didn't open session with publisher role" in { f =>
    pending
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
        publication.right.value.published must have (
          'requestId(1),
          'publicationId(2)
        )
      }
    }
  }


  it should "fail subscribe on topic if it turns out to be closed" in { f =>
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
}
