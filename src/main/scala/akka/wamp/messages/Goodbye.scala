package akka.wamp.messages

import akka.wamp._

/**
  * Sent by a [[Peer]] to close a previously opened [[Session]].  
  * Must be echo'ed by the receiving Peer.
  * 
  * ```
  * [GOODBYE, Details|dict, Reason|uri]
  * ```
  * 
  * @param details
  * @param reason
  */
case class Goodbye(details: Dict, reason: Uri) extends Message(GOODBYE)


/**
  * Build an [[Goodbye]] instance.
  */ 
class GoodbyeBuilder() extends Builder {
  var details: Dict = _
  var reason: Uri = _
  
  def build(): Message = {
    require(details != null, "missing details dict")
    require(reason != null, "missing reason uri")
    new Goodbye(details, reason)
  }
}