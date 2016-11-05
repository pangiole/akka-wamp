package akka.wamp.serialization

import akka.stream.scaladsl.Source
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken._
import com.fasterxml.jackson.databind.ObjectMapper

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

/**
  * This class is NOT THREAD SAFE!
  */
class JsonTextLazyPayload private[serialization](val unparsed: Source[String, _], parser: JsonParser, mapper: ObjectMapper)(implicit val executionContext: ExecutionContext) extends TextLazyPayload {
  import scala.collection.mutable

  override
  private[wamp] def args: Future[List[Any]] = parsed.map { case (args, _) => args }
  
  override
  private[wamp] def kwargs: Future[Map[String, Any]] = parsed.map { case (_, kwargs) => kwargs }

  override
  private[wamp] def kwargs[T](implicit ctag: ClassTag[T]): Future[T] = Future {
    def parse() = {
      if (parser.nextToken() == START_ARRAY) {
        parse_getValueAsArray()
        val token = parser.nextToken()
        if (token != null && token == START_OBJECT) {
          val clazz = ctag.runtimeClass
          mapper.readValue(parser, clazz).asInstanceOf[T]
        }
        else fail("Arguments|dict")
      }
      else fail("Arguments|list")  
    }
    val obj = parse()
    parser.close()
    obj
  }

  lazy
  private val parsed: Future[(List[Any], Map[String, Any])] = Future {
    var args = List.empty[Any]
    var kwargs = Map.empty[String, Any]
    if (parser.nextToken() == START_ARRAY) {
      args = parse_getValueAsArray()
      val token = parser.nextToken()
      if (token != null && token == START_OBJECT) {
        kwargs = parse_getValueAsObject()
      }
      parser.close()
    }
    else ()
    (args, kwargs)
  }

  private def parse_getValueAsInteger(): Any = {
    val number = parser.getValueAsLong
    if (number >= Int.MinValue && number <= Int.MaxValue) number.toInt
    else number
  }

  private def parse_getValueAsArray(): List[Any] = {
    val args = mutable.ListBuffer.empty[Any]
    while (parser.nextToken() != END_ARRAY) {
      args += parse_getValue()
    }
    args.toList
  }

  private def parse_getValueAsObject(): Map[String, Any] = {
    val kwargs = mutable.HashMap.empty[String, Any]
    while (parser.nextToken() != END_OBJECT) {
      val name = parser.getCurrentName()
      parser.nextToken()
      val value = parse_getValue()
      kwargs += name -> value
    }
    kwargs.toMap
  }

  private def parse_getValue(): Any = {
    val currentToken = parser.getCurrentToken()
    currentToken match {
      case VALUE_NUMBER_INT => parse_getValueAsInteger()
      case VALUE_NUMBER_FLOAT => parser.getValueAsDouble()
      case VALUE_STRING => parser.getValueAsString()
      case VALUE_FALSE | VALUE_TRUE => parser.getValueAsBoolean()
      case VALUE_NULL => null
      case START_ARRAY => parse_getValueAsArray()
      case START_OBJECT => parse_getValueAsObject()
      case n => fail(???)
    }
  }

  private def fail(field: String) = throw new DeserializeException(s"Expected $field but ${parser.getCurrentToken} found")

}