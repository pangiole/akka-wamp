package akka.wamp.client

import akka.wamp.messages.Abort

final case class TransportException(message: String) extends Throwable(message)

final case class SessionException(message: String) extends Throwable(message)

final case class AbortException(abort: Abort) extends Throwable(abort.toString)



