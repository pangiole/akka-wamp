package akka.wamp.serialization

import akka.util.ByteString
import akka.wamp.Dict

import scala.concurrent.Future

/**
  * A payload
  */
sealed trait Payload {
  /**
    * @return (future of) arguments as list
    */
  def arguments: Future[List[Any]] = Future.successful(List())

  /**
    * @return (future of) arguments as dictionary
    */
  def argumentsKw: Future[Dict] = Future.successful(Dict())
}

/**
  * A payload companion object
  */
object Payload {
  
  /**
    * An eager payload with in-memory structured arguments 
    * ready to be serialized to binary or text formats
    */
  private[serialization] trait Eager extends Payload {
    val memArgs: List[Any] = List()
    val memArgsKw: Dict = Dict()

    override def toString: String = s"EagerPayload($memArgs,$memArgsKw)"
  }

  /**
    * Create an eager payload with given arguments 
    * 
    * @param args is the in-memory arguments as list
    * @return an eager payload
    */
  def apply(args: List[Any]) = new Payload.Eager {
    override val memArgs: List[Any] = args
  }

  /**
    * Create an eager payload with given arguments
    *
    * @param argsKw is the in-memory arguments as dictionary
    * @return an eager payload
    */
  def apply(argsKw: Dict) = new Payload.Eager {
    override val memArgsKw: Dict = argsKw
  }

  /**
    * Create an eager payload with given arguments
    *
    * @param args is the in-memory arguments as list
    * @param argsKw is the in-memory arguments as dictionary
    * @return an eager payload
    */
  def apply(args: List[Any], argsKw: Dict) = new Payload.Eager {
    override val memArgs: List[Any] = args
    override val memArgsKw: Dict = argsKw
  }
}



/**
  * A binary outgoing payload (from the client point of view) 
  */
abstract class BinaryPayload extends Payload {
  /**
    * @return contents of this payload as a stream
    */
  def source(): ByteString
  override def toString: String = "BinaryPayload(...)"
}

/**
  * A textual outgoing payload (from the client point of view) 
  */
abstract class TextPayload extends Payload {
  /**
    * @return contents of this payload as a stream
    */
  def source(): String
  override def toString: String = "TextPayload(...)"
}

