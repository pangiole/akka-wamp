package akka.wamp.serialization

import akka.stream._
import akka.stream.scaladsl._
import akka.util._
import akka.wamp._
import akka.wamp.messages._

class MsgpackSerialization extends Serialization {
  
  type T = ByteString

  def serialize(message: Message)(implicit mat: Materializer): ByteString = ???

  @throws(classOf[DeserializeException])
  def deserialize(source: Source[ByteString, _])(implicit validator: Validator, mat: Materializer): Message = ???
}
