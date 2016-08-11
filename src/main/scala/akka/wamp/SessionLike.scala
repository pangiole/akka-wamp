package akka.wamp

import akka.wamp.router.Router

/**
  * It is a transient conversation between two [[Peer]]s (tipically a
  * a [[Router]] and a [[Client]]) attached to a [[Realm]] and running over 
  * a [[TransportLike]].
  */
trait SessionLike