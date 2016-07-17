package akka.wamp

import akka.actor._

/**
  * The subscription of a Subscriber to a Topic
  * 
  * @param id is the subscription ID
  * @param subscribers are the Subscribers actor references
  * @param topic is the topic
  */
case class Subscription(id: Id, subscribers: Set[ActorRef], topic: Uri)

