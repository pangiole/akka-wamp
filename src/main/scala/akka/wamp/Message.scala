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
  
  def build(): Message
}

