package akka.wamp

import akka.actor.ActorRef

/**
  * The subscription of a [[Subscriber]] to a [[Topic]]
  * 
  * @param id is the subscription ID
  * @param clientRefs are the [[Subscriber]]s actor references
  * @param topic is the topic
  */
class Subscription(val id: Id, val clientRefs: Set[ActorRef], val topic: Uri)


object Subscription {
  def generateId(used: Map[Id, Any], id: Id) = id
}