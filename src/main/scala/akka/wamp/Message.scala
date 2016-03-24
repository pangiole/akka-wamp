package akka.wamp


/**
  * A message
  * 
  * @param tpy is the message type (e.g. ``1`` for HELLO)
  */
class Message(val tpy: Int) extends Signal


/**
  * Build a message instance
  */
trait MessageBuilder {
  
  def fail(message: String) = throw new IllegalArgumentException(message)
  
  def check(condition: Boolean, message: String) = if (!condition) fail(message)
  
  def build(): Message
}

