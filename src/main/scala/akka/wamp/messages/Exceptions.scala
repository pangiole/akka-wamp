package akka.wamp.messages


final case class UnexpectedException(message: String) extends Throwable(message)

final case class ConnectionException(message: String) extends Throwable(message)

final case class OpenException(message: String) extends Throwable(message)

final case class AbortException(abort: Abort) extends Throwable(abort.toString)

final case class ErrorException(error: Error) extends Throwable(error.toString)



