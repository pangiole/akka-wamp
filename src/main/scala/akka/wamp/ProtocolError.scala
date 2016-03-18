package akka.wamp


class ProtocolError(message: String, cause: Throwable) extends Exception(message, cause) {
  
  def this(message: String) = this(message, null)
}
