package akka.wamp

import akka.actor.Actor

/**
  * A Peer communicates with another Peer by exchanging [[Message]]s 
  * during a transient [[Session]] established over a [[Transport]].
  * 
  * Each Peer MUST implement one [[Role]], and MAY implement more [[Role]]s.
  */
trait Peer extends Actor
