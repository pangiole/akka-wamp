package akka.wamp

import akka.wamp.messages._

/**
  * A Transport connects two [[Peer]]s and provides a channel over which 
  * [[Message]]s for a [[Session]] can flow in both directions.
  */
trait Transport
