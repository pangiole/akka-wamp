package akka.wamp


/**
  * A message
  * 
  * @param code is the message code (e.g. ``1`` for HELLO)
  */
class Message(val code: Int) extends Signal


/**
  * Build a message instance
  */
trait MessageBuilder {
  
  def build(): Message
}

