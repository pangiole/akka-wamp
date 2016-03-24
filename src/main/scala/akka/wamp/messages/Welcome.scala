package akka.wamp.messages

import akka.wamp._

/**
  * Sent by a [[Router]] to accept a [[Client]] to let it know the [[Session]] is now open
  * 
  * ```
  * [WELCOME, Session|id, Details|dict]
  * ```
  * 
  * 
  * @param sessionId
  * @param details
  */
case class Welcome(sessionId: Long, details: Dict) extends Message(WELCOME)
