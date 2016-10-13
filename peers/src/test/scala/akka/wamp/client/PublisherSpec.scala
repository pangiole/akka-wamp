package akka.wamp.client

import akka.Done
import org.scalamock.scalatest.MockFactory

class PublisherSpec extends ClientBaseSpec with MockFactory {

  "A publisher" should "fail publish to topic when session closed" in { f =>
    f.withSession { session =>
      whenReady(session.close()) { _ =>
        val publication = session.publish("myapp.topic")
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


  it should "succeed publish(ack) to topic and return publication" in { f =>
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
}
