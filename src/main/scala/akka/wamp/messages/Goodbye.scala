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
  */
case class Goodbye(details: Dict, reason: Uri) extends Message(GOODBYE)

class GoodbyeBuilder() extends Builder {
  var details: Dict = _
  var reason: Uri = _
  override def build(): Message = {
    Goodbye(details, reason)
  }
}