package akka.wamp.client

import akka.Done
import akka.actor.Actor._
import akka.wamp._
import akka.wamp.messages._
import akka.wamp.serialization._

import scala.collection.mutable
import scala.concurrent.Future

/**
  * Publisher is a client which publishes events to topics
  * without knowledge of subscribers
  */
trait Publisher { this: Session =>

  /** The event publications map */
  private val pendingPublications: PendingPublications = mutable.Map()

  /**
    * Publish an event to the given topic with the given (option of) payload
    *
    * {{{
    *    ,---------.          ,------.          ,----------.
    *   |Publisher|          |Broker|          |Subscriber|
    *   `----+----'          `--+---'          `----+-----'
    *        |     PUBLISH      |                   |
    *        |------------------>                   |
    *        |                  |                   |
    *        |PUBLISHED or ERROR|                   |
    *        |<------------------                   |
    *        |                  |                   |
    *        |                  |       EVENT       |
    *        |                  | ------------------>
    *   ,----+----.          ,--+---.          ,----+-----.
    *   |Publisher|          |Broker|          |Subscriber|
    *   `---------'          `------'          `----------'
    * }}}
    *
    * @param topic is the topic to publish to
    * @param ack is the acknowledge boolean switch (default is ``false``)
    * @param payload is the (option of) payload (default is ``None``)
    * @return either done or a (future of) publication 
    */
  def publish(topic: Uri, ack: Boolean = false, payload: Option[Payload] = None): Future[Either[Done, Publication]] =  {
    withPromise[Either[Done, Publication]] { promise =>
      val message = Publish(requestId = nextId(), Dict().setAck(ack), topic, payload)
      pendingPublications += (message.requestId -> promise)
      if (!ack) {
        connection ! message
        promise.success(Left(Done))
      }
      else {
        connection ! message
      }
    }
  }
  

  protected def handlePublicationSuccess: Receive = {
    case msg @ Published(requestId, _) =>
      log.debug("<-- {}", msg)
      pendingPublications.get(requestId).map { promise =>
        pendingPublications -= requestId
        promise.success(Right(Publication(msg)))
      }
  }


  protected def handlePublicationError: Receive = {
    case msg @ Error(Publish.tpe, requestId, _, error, _) =>
      log.debug("<-- {}", msg)
      pendingPublications.get(requestId).map { promise =>
        pendingPublications -= requestId  
        promise.failure(new SessionException(error))
      }
  }
}

case class Publication(published: Published)
