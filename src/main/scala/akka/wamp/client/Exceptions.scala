package akka.wamp.client

import akka.wamp.messages._

final case class ConnectionException(message: String) extends Throwable(message)

final case class TransportException(message: String) extends Throwable(message)

final case class SessionException(message: String) extends Throwable(message)

final case class AbortException(abort: Abort) extends Throwable(abort.toString)

final case class ErrorException(error: Error) extends Throwable(error.toString)



