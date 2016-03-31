package akka.wamp.messages

import akka.wamp._

/**
  * Acknowledge sent by a [[Broker]] to a [[Subscriber]] to acknowledge a subscription.
  * 
  * ```
  * [SUBSCRIBED, SUBSCRIBE.Request|id, Subscription|id]
  * ```
  *
  * @param request is the ID from the original [[Subscribe]] request
  * @param subscription is an ID chosen by the [[Broker]] for the subscription
  */
case class Subscribed(request: Id, subscription: Id) extends Message(SUBSCRIBED)