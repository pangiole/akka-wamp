package akka.wamp.serialization

import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

import akka.stream._
import akka.stream.scaladsl._
import akka.util.ByteString
import akka.wamp._
import akka.wamp.messages._
import com.fasterxml.jackson.core.JsonToken._
import com.fasterxml.jackson.core.{JsonFactory, JsonParser}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.concurrent.Future


class JsonSerialization() extends Serialization {
  type T = String

  private val log = LoggerFactory.getLogger(classOf[JsonSerialization])

  private val parserFactory = new JsonFactory()
  private val mapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)
  
  
  @throws(classOf[DeserializeException])
  override def deserialize(source: Source[String, _])(implicit validator: Validator, materializer: Materializer): Message = {
    implicit val ec = materializer.executionContext

    // We have to convert the Akka Stream Source given as input argument 
    // to an old fashioned java.io.InputStream because 
    // Jackson Streaming Parser requires it to read chars from. 

    val inputStream = source.
      map(ByteString(_)).
      runWith(StreamConverters.asInputStream())
    
    // Create the Jackson Streaming Parser
    val parser = parserFactory.createParser(inputStream)

    // Lazily create a Message
    def make(maker: => Message) = try { maker } catch { case ex: Throwable => throw new DeserializeException(ex.getMessage, ex)}
    
    def fail(token: String) = throw new DeserializeException(s"Expected $token but ${parser.getCurrentToken} found")

    /**
      * It lazily parse JSON payload
      */
    class JsonTextLazyPayload(val unparsed: Source[String, _]) extends TextLazyPayload {
      
      def valueOf(v: Any) = (if (v == null) None else v)
      
      lazy override val parsed: Future[ParsedContent] = Future {
        // """...[null,"paolo",40,true],{"height":1.65,"1":"pietro"}]"""
        var args = mutable.ListBuffer.empty[Any]
        val kwargs = mutable.HashMap.empty[String, Any]
        // TODO better to use Jackson Mapper here rather than Jackson Streaming Parser (which should have been closed)
        if (parser.getCurrentToken() == START_ARRAY) {
          while (parser.nextToken() != END_ARRAY) {
            val v = mapper.readValue(parser, classOf[Object])
            args += valueOf(v)
          }
          // TODO test with malformed JSON """[null,"paolo",40,true !"""
          if (parser.nextToken() == START_OBJECT) {
            while (parser.nextToken() != END_OBJECT) {
              val name = parser.getCurrentName
              parser.nextToken() // move next
              val v = mapper.readValue(parser, classOf[Object])
              kwargs += (name -> valueOf(v))
            }
            // TODO test with malformed JSON """[null,"paolo",40,true],{"height":1.65,1 !}"""
          }
          parser.close()
          //inputStream.close()
        }
        else fail("Arguments|list")
        ParsedContent(args.toList, kwargs.toMap)
      }
    }

    implicit class RichParser(parser: JsonParser) {
      def getValueAsDict(): Dict = {
        mapper.readValue(parser, classOf[Map[String, Map[_, _]]])
      }

      def getValueAsPayload(source: Source[String, _]): JsonTextLazyPayload = {
        // Get the chars count actually read and consumed by the 
        // Jackson Streaming Parser and drop those from the input
        // Akka Stream source
        val offset = parser.getCurrentLocation.getByteOffset
        val unparsed = source.drop(offset)
        
        // Up to this point, the Jackson Streaming Parser must have read 
        // (and buffered) a certain number of characters from its old
        // fashioned java.io.InputStream without consuming them yet.
        //
        val buffered = {
          val out = new ByteArrayOutputStream()
          val released = parser.releaseBuffered(out)
          val byteArray = out.toByteArray
          assert(byteArray.length == released)
          new String(byteArray, Charset.forName("UTF-8"))
        }

        // Hence, we need to push those buffered chars back to the 
        // Akka Stream unparsed source (prepending them)
        new JsonTextLazyPayload(
          // Create a new source with 
          // the buffered chars prepended
          // and the unparsed source as tail
          Source.single("[" + buffered).concat(unparsed)
        )
      }
    }

    implicit class RichField(field: String) {
      def |(dict: Dict.type): Dict =
        if (parser.nextToken() == START_OBJECT) parser.getValueAsDict
        else fail(s"$field|dict")

      def |(id: Id.type): Id =
        if (parser.nextToken() == VALUE_NUMBER_INT) parser.getValueAsLong
        else fail(s"$field|id")

      def |(uri: Uri.type): Uri =
        if (parser.nextToken() == VALUE_STRING) parser.getValueAsString
        else fail(s"$field|uri")

      def |(int: Int.type): Int =
        if (parser.nextToken() == VALUE_NUMBER_INT) parser.getValueAsInt
        else fail(s"$field|int")

      def |(p: Payload.type): Payload =
        if (parser.nextToken() == START_ARRAY) {
          parser.getValueAsPayload(source)
        }
        else {
          // WARN: DO NOT return neither Source.empty nor Source.single("") 
          new JsonTextLazyPayload(Source.single("[]"))
        }
    }

    if (parser.nextToken() == START_ARRAY) {
      if (parser.nextToken() == VALUE_NUMBER_INT) {
        val tpe = parser.getIntValue
        tpe match {
          case Hello.tpe        => make(Hello("Realm"|Uri, "Details"|Dict))
          case Welcome.tpe      => make(Welcome("Session"|Id, "Details"|Dict))
          case Abort.tpe        => make(Abort("Details"|Dict, "Reason"|Uri))
          case Goodbye.tpe      => make(Goodbye("Details"|Dict, "Reason"|Uri))
          case Error.tpe        => make(Error("REQUEST.Type"|Int, "REQUEST.Request"|Id, "Details"|Dict, "Error"|Uri, "Arguments"|Payload))
          case Publish.tpe      => make(Publish("REQUEST.Request"|Id, "Details"|Dict, "Topic"|Uri, "Arguments"|Payload))
          case Published.tpe    => make(Published("PUBLISH.Request"|Id, "Publication"|Id))
          case Subscribe.tpe    => make(Subscribe("Request"|Id, "Options"|Dict, "Topic"|Uri))
          case Subscribed.tpe   => make(Subscribed("SUBSCRIBE.Request"|Id, "Subscription"|Id))
          case Unsubscribe.tpe  => make(Unsubscribe("Request"|Id, "SUBSCRIBE.Subscription"|Id))
          case Unsubscribed.tpe => make(Unsubscribed("UNSUBSCRIBE.Request"|Id))
          case Event.tpe        => make(Event("SUBSCRIBED.Subscription"|Id, "PUBLISHED.Publication"|Id, "Details"|Dict, "Arguments"|Payload))
          case Register.tpe     => make(Register("Request"|Id, "Options"|Dict, "Procedure"|Uri))
          case Registered.tpe   => make(Registered("REGISTER.Request"|Id, "Registration"|Id))
          case Unregister.tpe   => make(Unregister("Request"|Id, "REGISTER.Registration"|Id))
          case Unregistered.tpe => make(Unregistered("UNREGISTER.Request"|Id))
          case Call.tpe         => make(Call("Request"|Id, "Options"|Dict, "Procedure"|Uri, "Arguments"|Payload))
          case Invocation.tpe   => make(Invocation("Request"|Id, "REGISTERED.Registration"|Id, "Details"|Dict, "Arguments"|Payload))
          case Yield.tpe        => make(Yield("INVOCATION.Request"|Id, "Options"|Dict, "Arguments"|Payload))
          case Result.tpe       => make(Result("CALL.Request"|Id, "Details"|Dict, "YIELD.Arguments"|Payload))
          case _                => fail("MessageType|Integer")
        }
      }
      else fail("MessageType|Integer")
    }
    else fail("START_ARRAY")
  }

  
  override def serialize(message: Message): Source[String, _] = {
    def toJson(elem: Any): String = {
      elem match {
        case Some(v) => toJson(v)
        case list: List[_] => list.map(toJson).mkString("[", ",", "]")
        case dict: Map[_, _] => dict.map { case (k, v) => s""""${k}":${toJson(v)}""" }.mkString("{", ",", "}")
        case str: String => s""""$str""""
        case Symbol(name) => s""""$name""""
        case None | null => null
        case any => any.toString
      }
    }

    val elems = 
      message match {
        case Hello(realm, details)                          => Hello.tpe :: realm :: details :: None :: Nil
        case Welcome(sessionId, details)                    => Welcome.tpe :: sessionId :: details :: None :: Nil
        case Goodbye(details, reason)                       => Goodbye.tpe :: details :: reason :: None :: Nil
        case Abort(details, reason)                         => Abort.tpe :: details :: reason :: None :: Nil
        case Error(reqType, reqId, details, error, payload) => Error.tpe :: reqType :: reqId :: details :: error :: Some(payload) :: Nil
        case Publish(requestId, options, topic, payload)    => Publish.tpe :: requestId :: options :: topic :: Some(payload) :: Nil
        case Published(requestId, publicationId)            => Published.tpe :: requestId :: publicationId :: None :: Nil
        case Subscribe(requestId, options, topic)           => Subscribe.tpe :: requestId :: options :: topic :: None :: Nil
        case Subscribed(requestId, subscriptionId)          => Subscribed.tpe :: requestId :: subscriptionId :: None :: Nil
        case Unsubscribe(requestId, subscriptionId)         => Unsubscribe.tpe :: requestId :: subscriptionId :: None :: Nil
        case Unsubscribed(requestId)                        => Unsubscribed.tpe :: requestId :: None :: Nil
        case Event(subId, pubId, details, payload)          => Event.tpe :: subId :: pubId :: details :: Some(payload) :: Nil
        case Register(requestId, options, procedure)        => Register.tpe :: requestId :: options :: procedure :: None :: Nil
        case Registered(requestId, registrationId)          => Registered.tpe :: requestId :: registrationId :: None :: Nil
        case Unregister(requestId, registrationId)          => Unregister.tpe :: requestId :: registrationId :: None :: Nil
        case Unregistered(requestId)                        => Unregistered.tpe :: requestId :: None :: Nil
        case Call(requestId, options, procedure, payload)   => Call.tpe :: requestId :: options :: procedure :: Some(payload) :: Nil
        case Invocation(reqId, regstrId, options, payload)  => Invocation.tpe :: reqId :: regstrId :: options :: Some(payload) :: Nil
        case Yield(requestId, options, payload)             => Yield.tpe :: requestId :: options :: Some(payload) :: Nil
        case Result(requestId, details, payload)            => Result.tpe :: requestId :: details :: Some(payload) :: Nil
      }

    
    val (fields, payload) = (elems.dropRight(1), elems.last) 
    payload match {
      case None =>
        Source.single(fields.map(toJson).mkString("[", ",", "]"))

      case Some(p: TextLazyPayload) =>
        Source.single(fields.map(toJson).mkString("[", ",", ",")).concat(p.unparsed).concat(Source.single("]"))
        
      case Some(p: BinaryLazyPayload) => 
        throw new IllegalStateException("Cannot serialize binary payload to JSON")
        
      case Some(p: EagerPayload) =>
        val fieldsAndPayload = 
          if (p.content.args.isEmpty && p.content.kwargs.isEmpty) {
            fields
          }  
          else if (p.content.kwargs.isEmpty) {
            fields ::: p.content.args :: Nil
          }
          else {
            fields ::: p.content.args :: p.content.kwargs :: Nil
          }
        Source.single(fieldsAndPayload.map(toJson).mkString("[", ",", "]"))
    }
  }
}


