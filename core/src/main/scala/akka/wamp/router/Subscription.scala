package akka.wamp.router

import akka.actor._
import akka.wamp._

/**
  * The subscription of a subscriber to a topic
  * 
  * @param id is this subscription identifier
  * @param subscribers are the subscriber actors references
  * @param topic is the subscribed topic identifier
  */
case class Subscription(id: Id, subscribers: Set[ActorRef], topic: Uri)

