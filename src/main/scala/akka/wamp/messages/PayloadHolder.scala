package akka.wamp.messages

import akka.wamp.serialization.Payload

/**
  * A message that contains a payload
  */
trait PayloadHolder { this: Message =>
  
  /**
    * @return the payload of this message
    */
  def payload: Payload
}
