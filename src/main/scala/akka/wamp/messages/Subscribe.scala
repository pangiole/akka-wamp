package akka.wamp.messages

import akka.wamp._

/**
  * Subscribe request sent by a [[Subscriber]] to a [[Broker]] to subscribe to a [[Topic]].
  * 
  * ```
  * [SUBSCRIBE, Request|id, Options|dict, Topic|uri]
  * ```
  *
  * @param request is a random, ephemeral ID chosen by the [[Subscribe]] and used to correlate the [[Broker]]'s response with the request
  * @param options is a dictionary that allows to provide additional subscription request details in a extensible way
  * @param topic is the topic the [[Subscribe]]  wants to subscribe to 
  */
case class Subscribe(request: Id, options: Dict, topic: Uri) extends Message(SUBSCRIBE)



/**
  * Build an [[Subscribe]] instance.
  */
class SubscribeBuilder() extends Builder {
  var request: Id = -1
  var options: Dict = _
  var topic: Uri = _
  override def build(): Message = {
    require(request != -1, "missing request id")
    require(options != null, "missing options dict")
    require(topic != null, "missing topic uri")
    new Subscribe(request, options, topic)
  }
}