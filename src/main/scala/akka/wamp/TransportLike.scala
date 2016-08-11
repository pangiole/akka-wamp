package akka.wamp

import akka.wamp.messages.Message

/**
  * A Transport connects two [[Peer]]s and provides a channel over which 
  * [[Message]]s for a [[SessionLike]] can flow in both directions.
  */
trait TransportLike
