package akka.wamp.serialization

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.nio.charset.Charset

import akka.stream.Materializer
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

  private val UTF8 = Charset.forName("UTF-8")

  @throws(classOf[DeserializeException])
  override def deserialize(source: String)(implicit validator: Validator, materializer: Materializer): Message = {
    implicit val ec = materializer.executionContext
    log.trace("Deserializing {}", source)

    val factory = new JsonFactory()
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)

    // lazy evaluation of input argument
    def make(factory: => Message) = try { factory } catch { case ex: Throwable => throw new DeserializeException(ex.getMessage, ex)}

    val inputStream = new ByteArrayInputStream(source.getBytes(UTF8))
    val parser = factory.createParser(inputStream)

    def fail(token: String) = throw new DeserializeException(s"Expected $token but ${parser.getCurrentToken} found")

    // lazy payload
    class JsonTextPayload(val source: String) extends TextPayload {
      lazy override val arguments: Future[List[Any]] = Future {
        if (parser.getCurrentToken() == START_ARRAY) {
          val buffer = mutable.ListBuffer.empty[Any]
          while (parser.nextToken() != END_ARRAY) {
            val value = mapper.readValue(parser, classOf[Object])
            buffer += value
          }
          if (parser.nextToken() != START_OBJECT) {
            parser.close()
            //inputStream.close()
          }
          buffer.toList
        }
        else fail("Arguments|list")
      }

      lazy override val argumentsKw: Future[Dict] = {
        arguments.map { args =>
          if (parser.getCurrentToken() == START_OBJECT) {
            val buffer = mutable.Map.empty[String, Any]
            while (parser.nextToken() != END_OBJECT) {
              val name = parser.getCurrentName
              parser.nextToken() // move next
              val value = mapper.readValue(parser, classOf[Object])
              buffer += (name -> value)
            }
            parser.close()
            //inputStream.close()
            val map1 = args.zipWithIndex.map { case (a, i) => s"arg$i" -> a }.toMap
            map1 ++ buffer.toMap
          }
          else fail("ArgumentsKw|dict")
        }
      }
    }

    implicit class RichParser(parser: JsonParser) {
      def getValueAsDict(): Dict = {
        mapper.readValue(parser, classOf[Map[String, Map[_, _]]])
      }

      def getValueAsPayload(source: String): JsonTextPayload = {
        // Up to this point, the Jackson Streaming Parser must have read 
        // (and buffered) a certain number of characters from the input 
        // Akka Stream Source without consuming them yet.
        //
        // Therefore, we need to push those buffered chars back and prepend
        // them to the Akka Stream Source originally given as input 
        val buffered = {
          val out = new ByteArrayOutputStream()
          val released = parser.releaseBuffered(out)
          val byteArray = out.toByteArray
          assert(byteArray.length == released)
          new String(byteArray, UTF8)
        }

        // Get the chars number actually read and consumed by the 
        // Jackson Streaming Parser and drop those from the input
        // Akka Stream source
        val offset = parser.getCurrentLocation.getByteOffset
        val remaining = source.drop(offset.toInt + buffered.length)

        new JsonTextPayload("[" + buffered + remaining)
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

      def |(p: Payload.type): Option[Payload] =
        if (parser.nextToken() == START_ARRAY) Some(parser.getValueAsPayload(source))
        else None
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
          case _                => fail("MessageType|Integer")
        }
      }
      else fail("MessageType|Integer")
    }
    else fail("START_ARRAY")
  }


  def serialize(message: Message): String = {
    log.trace("Serializing {}", message)

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

    val (elems, payload) = 
      message match {
        case Hello(realm, details)                          => (Hello.tpe :: realm :: details :: Nil, None)
        case Welcome(sessionId, details)                    => (Welcome.tpe :: sessionId :: details :: Nil, None)
        case Goodbye(details, reason)                       => (Goodbye.tpe :: details :: reason :: Nil, None)
        case Abort(details, reason)                         => (Abort.tpe :: details :: reason :: Nil, None)
        case Error(reqType, reqId, details, error, payload) => (Error.tpe :: reqType :: reqId :: details :: error :: Nil, payload)
        case Publish(requestId, options, topic, payload)    => (Publish.tpe :: requestId :: options :: topic :: Nil, payload)
        case Published(requestId, publicationId)            => (Published.tpe :: requestId :: publicationId :: Nil, None)
        case Subscribe(requestId, options, topic)           => (Subscribe.tpe :: requestId :: options :: topic :: Nil, None)
        case Subscribed(requestId, subscriptionId)          => (Subscribed.tpe :: requestId :: subscriptionId :: Nil, None)
        case Unsubscribe(requestId, subscriptionId)         => (Unsubscribe.tpe :: requestId :: subscriptionId :: Nil, None)
        case Unsubscribed(requestId)                        => (Unsubscribed.tpe :: requestId :: Nil, None)
        case Event(subId, pubId, details, payload)          => (Event.tpe :: subId :: pubId :: details :: Nil, payload)
        case Register(requestId, options, procedure)        => (Register.tpe :: requestId :: options :: procedure :: Nil, None)
        case Registered(requestId, registrationId)          => (Registered.tpe :: requestId :: registrationId :: Nil, None)
        case Unregister(requestId, registrationId)          => (Unregister.tpe :: requestId :: registrationId :: Nil, None)
        case Unregistered(requestId)                        => (Unregistered.tpe :: requestId :: Nil, None)
      }

    payload match {
      case None => 
        elems.map(toJson).mkString("[", ",", "]")
        
      case Some(p: TextPayload) => 
        elems.map(toJson).mkString("[", ",", ",").concat(p.source).concat("]")
        
      case Some(p: BinaryPayload) => 
        throw new IllegalStateException("Cannot serialize binary payload to JSON")
        
      case Some(p: Payload.Eager) =>
        val all = 
          if (p.memArgsKw.isEmpty) elems ::: p.memArgs :: Nil
          else elems ::: p.memArgs :: p.memArgsKw :: Nil

        all.map(toJson).mkString("[", ",", "]")
    }
  }
}


