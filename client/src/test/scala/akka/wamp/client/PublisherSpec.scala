/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.md', which is part of this source code package.
 */

package akka.wamp.client

import org.scalamock.scalatest.MockFactory


class PublisherSpec extends DefaultSpec with MockFactory {
  "A client.Publisher" should "fail to publish to topic when session closed" in { f =>
    f.withSession { session =>
      whenReady(session.close()) { _ =>
        val publication = session.publishAck("mytopic")
        whenReady(publication.failed) { ex =>
          ex mustBe a[ClientException]
          ex.getMessage mustBe "Session closed"
        }
      }
    }
  }


  it should "succeed to publish to topic" in { f =>
    f.withSession { session =>
      session.publish("mytopic")
      assert(true)
    }
  }


  it should "fail to publish(Ack) to unknown topic" in { f =>
    pending
    f.withSession { session =>
      val publication = session.publishAck("unknown")
      whenReady(publication.failed) { ex =>
        ex mustBe a[ClientException]
        ex.getMessage mustBe "Unknown topic"
      }
    }
  }


  it should "succeed to publish to topic with acknowledgment pattern" in { f =>
    f.withSession { session =>
      val publication = session.publishAck("mytopic")
      whenReady(publication) { publication =>
        publication.ack must have(
          'requestId (1),
          'publicationId (2)
        )
      }
    }
  }

  // TODO test publish some payloads
}
