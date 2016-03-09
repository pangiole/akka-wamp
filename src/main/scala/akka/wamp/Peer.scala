package akka.wamp

/**
  * A Peer communicates with another Peer by exchanging [[Message]]s 
  * during a transient [[Session]] established over a [[WebsocketTransport]].
  * 
  * Each Peer MUST implement one [[Role]], and MAY implement more [[Role]]s.
  */
trait Peer 
