/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.md', which is part of this source code package.
 */

package akka.wamp.serialization

import akka.stream.scaladsl.Source
import akka.util.ByteString

import scala.concurrent.Future
import scala.reflect.ClassTag


/**
  * Represents either an eager or lazy payload conveyed by a [[akka.wamp.messages.DataConveyor]].
  *
  * [[EagerPayload]] instances can be created using its companion object.
  *
  * [[LazyPayload]] instances will be created by Akka Wamp on incoming messages.
  *
  * @see [[EagerPayload]]
  * @see [[LazyPayload]]
  *
  */
abstract class Payload


/**
  * Factory for [[EagerPayload]] instances.
  *
  * {{{
  *   // empty payload
  *   val empty = Payload()
  *
  *   // conveying a list of indexed arguments
  *   val indexed = Payload(List("paolo", 99, true))
  *
  *   // conveying a dictionary of named arguments
  *   val named = Payload(Map(
  *     "name" -> "paolo",
  *     "age" -> 99,
  *     "male" -> true
  *   ))
  * }}}
  *
  */
object Payload {
  
  /**
    * Creates an eager payload with empty content
    *
    * @return the new payload
    */
  def apply() = {
    new EagerPayload(List(), Map())
  }

  /**
    * Creates an eager payload with the given list of indexed arguments
    * 
    * @param args is the list of indexed arguments
    * @return the new payload
    */
  def apply(args: List[Any]) = {
    new EagerPayload(args, Map())
  }

  /**
    * Creates an eager payload with the given dictionary of named arguments
    *
    * @param kwargs is the dictionary of named arguments
    * @return the new payload
    */
  def apply(kwargs: Map[String, Any]) = {
    new EagerPayload(List(), kwargs)
  }

  /**
    * Creates a payload with both arguments list and dictionary
    * 
    * @param args is the list of arguments of arbitrary type
    * @param kwargs is the dictionary of arguments of arbitrary type
    * @return the new payload
    */
  @deprecated("Not a good practice")
  def apply(args: List[Any], kwargs: Map[String, Any]) = {
    new EagerPayload(args, kwargs)
  }
}


/**
  * Represents a payload whose content whose content don't need to be parsed.
  */
class EagerPayload private[wamp](val args: List[Any], val kwargs: Map[String, Any]) extends Payload {
  
  override def toString: String = s"EagerPayload(${args}, ${kwargs})"

  def canEqual(other: Any): Boolean = other.isInstanceOf[EagerPayload]
  
  override def equals(other: Any): Boolean = other match {
    case that: EagerPayload =>
      (that canEqual this) &&
        args == that.args &&
        kwargs == that.kwargs
    case _ => false
  }
  
  override def hashCode(): Int = {
    val state = Seq(args, kwargs)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}

/**
  * Represents a payload whose content has not been parsed yet.
  *
  * You can parse this payload contents as follows:
  *
  * {{{
  *   // val payload: Payload = ...
  *   payload match {
  *     case p: TextLazyPayload =>
  *       val unparsed: Source[String, _] = p.unparsed
  *       // parse textual source ...
  *
  *     case p: BinaryLazyPayload =>
  *       val unparsed: Source[ByteString, _] = p.unparsed
  *       // parse binary source ...
  *
  *     case _ => ()
  *   }
  * }}}
  */
sealed trait LazyPayload[+T] extends Payload {

  private[wamp] def args: Future[List[Any]]

  private[wamp] def kwargs: Future[Map[String, Any]]

  private[wamp] def kwargs[T](implicit ctag: ClassTag[T]): Future[T]

  /**
    * Returns the unparsed content as stream source
    *
    * @return
    */
  def unparsed(): Source[T, _]


  override def toString: String = "LazyPayload(?)"
}


/**
  * Represents a lazy payload with binary content
  */
trait BinaryLazyPayload extends LazyPayload[ByteString]


/**
  * Represents a lazy payload with textual content
  */
trait TextLazyPayload extends LazyPayload[String]

