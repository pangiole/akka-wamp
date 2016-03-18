package akka.wamp

import akka.actor.ActorRef

/**
  * A Session is a transient conversation between two [[Peer]]s attached to a
  * [[Realm]] and running over a [[Transport]].
  * 
  * A session connects two [[Peer]]s, a [[Client]] and a [[Router]].
  * 
  * @param id
  * @param peer1
  * @param peer2
  */
class Session(var id: Long, var peer1: ActorRef, var peer2: ActorRef)
