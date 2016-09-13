package akka.wamp

import akka.wamp.client._
import akka.wamp.messages.Message
import akka.wamp.router._

/**
  * A Peer communicates with another Peer by exchanging [[Message]]s 
  * during a transient [[SessionLike]] established over a [[Connection]].
  * 
  * A Peer could be either a [[Client]] or a [[Router]] 
  *  - it must implement one [[Role]], and
  *  - may implement more [[Role]]s.
  */
trait Peer

