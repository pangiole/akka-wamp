package akka.wamp.client.japi

import akka.wamp.serialization.{Payload => PayloadDelegate}

import scala.collection.JavaConverters._
import java.{util => ju}

import akka.wamp.serialization

/**
  * Represents a message payload.
  *
  * [[EagerPayload]] instances can be created using its companion object.
  *
  * [[LazyPayload]] instances will be created by Akka Wamp on incoming messages.
  *
  * @note Java API
  * @see [[EagerPayload]]
  * @see [[LazyPayload]]
  */
class Payload private[japi](private[japi] val delegate: serialization.Payload)


/**
  * Factory for Payload instances.
  *
  * {{{
  *   import java.util.HashMap;
  *   import static java.util.Arrays.asList
  *
  *   // empty payload
  *   Payload empty = Payload.create();
  *
  *   // conveying a list of indexed arguments
  *   Payload indexed = Payload.create(asList("paolo", 99, true));
  *
  *   // conveying a dictionary of named arguments
  *   Payload named = Payload.create(new HashMap<String, Object>(){{
  *     put("name", "paolo");
  *     put("age", 99);
  *     put("male", true);
  *   }});
  * }}}
  *
  * @note Java API
  */
object Payload {

  /**
    * Creates an eager payload with no content
    *
    * @return the new payload
    */
  def create(): Payload = {
    new EagerPayload(delegate = PayloadDelegate.apply())
  }

  /**
    * Creates an eager payload with the given list of indexed arguments
    *
    * @param args is the list of indexed arguments
    * @return the new payload
    */
  def create(args: ju.List[Object]): Payload = {
    new EagerPayload(delegate = PayloadDelegate.apply(args.asScala.toList))
  }

  /**
    * Creates an eager payload with the given dictionary of named arguments
    *
    * @param kwargs is the dictionary of named arguments
    * @return the new payload
    */
  def create(kwargs: ju.Map[String, Object]): Payload = {
    new EagerPayload(delegate = PayloadDelegate.apply(kwargs.asScala.toMap))
  }

}


/**
  * Represents a payload whose content whose content don't need to be parsed.
  *
  * @note Java API
  */
class EagerPayload private[japi](delegate: serialization.EagerPayload) extends Payload(delegate)


class LazyPayload extends Payload(null)

/**
  * Represents a lazy payload with textual content
  */
class TextLazyPayload extends Payload(null) {
  // def unparsed(): Source[String, _] = ???
}

/**
  * Represents a lazy payload with binary content
  */
class BinaryLazyPayload extends Payload(null) {

  // def unparsed(): Source[ByteString, _] = ???
}
