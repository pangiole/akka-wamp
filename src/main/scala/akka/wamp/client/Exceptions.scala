package akka.wamp.client

import akka.wamp._
import akka.wamp.messages.Abort

final case class ConnectionException(message: String) extends Throwable(message)

final case class SessionException(message: String) extends Throwable(message)

final case class AbortException(abort: Abort) extends Throwable(abort.toString)



