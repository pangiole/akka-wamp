package akka.wamp

import akka.actor._

import scala.annotation.tailrec

/**
  * The subscription of a [[Subscriber]] to a [[Topic]]
  * 
  * @param id is the subscription ID
  * @param subscribers are the [[Subscriber]]s actor references
  * @param topic is the topic
  */
case class Subscription(id: Id, subscribers: Set[ActorRef], topic: Uri)



object Subscription {

  @tailrec
  def generateId(used: Map[Id, Any], id: Id = -1): Id = {
    if (id == -1 || used.isDefinedAt(id)) generateId(used, id + 1)
    else id
  }
  
  // def generateId(used: Map[Id, Any], id: Id) = id
}