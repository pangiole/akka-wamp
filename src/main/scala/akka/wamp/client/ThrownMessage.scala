package akka.wamp.client

import akka.wamp.Wamp.AbstractMessage

case class ThrownMessage(message: AbstractMessage) extends Throwable