package akka.wamp.client


/**
  * Represents an error
  *
  * @param message
  * @param cause
  */
case class ClientException(message: String, cause: Throwable) extends Throwable(message, cause)
