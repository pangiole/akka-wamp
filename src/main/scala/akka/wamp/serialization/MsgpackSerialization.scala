package akka.wamp.serialization

import akka.stream.Materializer
import akka.util.ByteString
import akka.wamp.Validator
import akka.wamp.messages._

class MsgpackSerialization extends Serialization {
  
  type T = ByteString

  def serialize(message: Message): ByteString = ???

  @throws(classOf[DeserializeException])
  def deserialize(source: ByteString)(implicit validator: Validator, materializer: Materializer): Message = ???
}
