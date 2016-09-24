package akka.wamp.messages

import akka.wamp.serialization.Payload

/**
  * A message that contains a payload
  */
trait PayloadContainer { this: Message =>
  
  /**
    * 
    * @return the payload of this message
    */
  def payload: Option[Payload]
}
