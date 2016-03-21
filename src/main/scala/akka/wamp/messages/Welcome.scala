package akka.wamp.messages

import akka.wamp._

case class Welcome(sessionId: Long, details: Dict) extends Message(WELCOME)
