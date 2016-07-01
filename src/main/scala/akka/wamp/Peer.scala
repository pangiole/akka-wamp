package akka.wamp

import akka.actor.ActorLogging


/**
  * A Peer communicates with another Peer by exchanging Messages 
  * during a transient Session established over a Transport.
  * 
  * Each Peer MUST implement one Role, and MAY implement more Roles.
  */
private[wamp] trait Peer { this:  ActorLogging =>
  
}

