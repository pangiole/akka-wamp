package akka.wamp.messages

import akka.wamp.Message


case class Welcome(sessionId: Long, details: Dict) extends Message(WELCOME)
