package akka.wamp


/**
  * A Peer communicates with another Peer by exchanging Messages 
  * during a transient Session established over a Transport.
  * 
  * Each Peer MUST implement one Role, and MAY implement more Roles.
  */
trait Peer

