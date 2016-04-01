package akka.wamp

import akka.actor._

/**
  * A Peer communicates with another Peer by exchanging [[Message]]s 
  * during a transient [[Session]] established over a [[Transport]].
  * 
  * Each Peer MUST implement one [[Role]], and MAY implement more [[Role]]s.
  */
abstract class Peer extends Actor with ActorLogging {
  
  val roles: Set[String]
}

