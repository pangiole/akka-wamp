package akka.wamp.messages

import akka.wamp._

/**
  * Sent by a [[Peer]] to abort the opening of a [[Session]].
  * No response is expected.
  * 
  * ```
  * [ABORT, Details|dict, Reason|uri]
  * ```
  */
case class Abort(details: Dict, reason: Uri) extends Message(ABORT)

