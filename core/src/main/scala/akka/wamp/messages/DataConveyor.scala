package akka.wamp.messages

import akka.wamp.serialization.{EagerPayload, LazyPayload, Payload}

import scala.reflect.ClassTag

/**
  * Represents a message conveying application data.
  *
  *
  * == Incoming conveyors ==
  *
  * {{{
  *   // val message: DataConveyor = ...
  *
  *   // Default parsers
  *   val args: Future[List[Any]] = message.args
  *   val kwargs: Future[Map[String, Any]] = message.kwargs
  *   val user: Future[UserType] = message.kwargs[UserType]
  *
  *   // Custom parsers
  *   val payload: Payload = message.payload
  *   payload match {
  *     case p: TextLazyPayload =>
  *       val unparsed: Source[String, _] = p.unparsed
  *       // ...
  *
  *     case p: BinaryLazyPayload =>
  *       val unparsed: Source[ByteString, _] = p.unparsed
  *       // ...
  *   }
  * }}}
  *
  */
trait DataConveyor { this: ProtocolMessage =>
  
  /**
    * Is the payload
    */
  def payload: Payload
  
  /**
    * Is the conveyed data as list of indexed arguments
    * 
    * @return the (future of) indexed args
    */
  def args: List[Any] = payload match {
    case p: EagerPayload   => p.args
    case p: LazyPayload[_] => p.args
  }

  /**
    * Is the conveyed data as dictionary of named arguments
    *
    * @return the (future of) named args
    */
  def kwargs: Map[String, Any] = payload match {
    case p: EagerPayload   => p.kwargs
    case p: LazyPayload[_] => p.kwargs
  }


  /**
    * Is the conveyed data as instance of the given user tpye
    *
    * @tparam T is the user type
    * @return the (future of) named args
    */
  def kwargs[T](implicit ctag: ClassTag[T]): T = payload match {
    case p: EagerPayload   => throw new IllegalStateException
    case p: LazyPayload[_] => p.kwargs[T]
  }

}
