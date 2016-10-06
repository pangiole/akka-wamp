package akka.wamp.serialization

import akka.stream.scaladsl.Source
import akka.util.ByteString

import scala.concurrent.Future

/**
  * Payload content parsed by using either the 
  * [Jackson JSON Parser](https://github.com/FasterXML/jackson-module-scala)
  * (for textual data) or the 
  * [MsgPack Parser](https://github.com/msgpack/msgpack-scala) 
  * (for binary data).
  * 
  * @param args is a list of arbitrary values
  * @param kwargs is a dictionary of arbitrary values
  */
case class ParsedContent(args: List[Any], kwargs: Map[String, Any]) {
  val isEmpty: Boolean = args.isEmpty && kwargs.isEmpty
}


/**
  * Payload
  */
trait Payload {
  /**
    * Let Akka Wamp lazily parse the content of this payload 
    * delegating to proper default parsers.
    * 
    * @return (future of) parsed content as dictionary of arbitrary types
    */
  def parsed: Future[ParsedContent] = Future.successful(ParsedContent(List(), Map()))
}


/**
  * A payload companion object
  */
object Payload {
  
  /**
    * Create a payload with empty content
    *
    * @return the new payload
    */
  def apply() = {
    new EagerPayload(ParsedContent(List(), Map()))
  }

  /**
    * Create a payload with arguments list
    * 
    * @param args is the list of arguments of arbitrary type
    * @return the new payload
    */
  def apply(args: List[Any]) = {
    new EagerPayload(ParsedContent(args, Map()))
  }

  /**
    * Create a payload with arguments dictionary
    *
    * @param kwargs is the dictionary of arguments of arbitrary type
    * @return the new payload
    */
  def apply(kwargs: Map[String, Any]) = {
    new EagerPayload(ParsedContent(List(), kwargs))
  }

  /**
    * Create a payload with both arguments list and dictionary
    * 
    * @param args is the list of arguments of arbitrary type
    * @param kwargs is the dictionary of arguments of arbitrary type
    * @return the new payload
    */
  def apply(args: List[Any], kwargs: Map[String, Any]) = {
    new EagerPayload(ParsedContent(args, kwargs))
  }
  
  val defaultPayload = new EagerPayload(ParsedContent(List(), Map()))
}

/**
  * An eager payload with in-memory structured arguments 
  * ready to be serialized to binary or text formats
  */
class EagerPayload private[serialization](val content: ParsedContent) extends Payload {

  override def parsed: Future[ParsedContent] = Future.successful(content)

  override def toString: String = s"EagerPayload($content)"

  def canEqual(other: Any): Boolean = other.isInstanceOf[EagerPayload]

  override def equals(other: Any): Boolean = other match {
    case that: EagerPayload =>
      (that canEqual this) &&
        content == that.content
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(content)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}

/**
  * A lazy payload
  * 
  * @tparam T 
  */
sealed trait LazyPayload[+T] extends Payload {
  /**
    * @return unparsed content as stream source
    */
  def unparsed(): Source[T, _]

  override def toString: String = "LazyPayload(...)"
}

/**
  * A lazy payload with binary content
  */
trait BinaryLazyPayload extends LazyPayload[ByteString]

/**
  * A lazy payload with textual content
  */
trait TextLazyPayload extends LazyPayload[String]

