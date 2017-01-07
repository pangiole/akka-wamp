package akka.wamp.serialization


import akka.stream.Materializer
import akka.stream.scaladsl.{Source, StreamConverters}
import akka.util.ByteString
import akka.wamp._
import akka.wamp.messages._
import com.fasterxml.jackson.core.JsonToken._
import com.fasterxml.jackson.core.{JsonFactory, JsonParser}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule


/**
  * This class is NOT THREAD SAFE!
  */
class JsonSerialization(jsonFactory: JsonFactory)(implicit materializer: Materializer) extends Serialization {
  type T = String

  // Create a new Jackson Object Mapper
  def mkObjectMapper(): ObjectMapper = {
    val mapper = new ObjectMapper(jsonFactory)
    // TODO avoid using DefaultScalaModule
    mapper.registerModule(DefaultScalaModule)  
    mapper
  }


  // Create a Jackson Streaming Parser from the given Akka Stream strSource
  def mkStreamingParser(strSource: Source[String, _]): JsonParser = {
    // We need to feed the Jackson Streaming Parser
    // with an old fashioned java.io.InputStream which
    // can be created from the given Akka Stream strSource

    // Note that we're NOT running the original strSource but
    // rather a new byteSource with a converter
    val byteSource = strSource.map(ByteString(_))
    val inputStream = byteSource.runWith(StreamConverters.asInputStream())
    jsonFactory.createParser(inputStream)
  }


  @throws(classOf[DeserializeException])
  override def deserialize(strSource: Source[String, _])(implicit validator: Validator, materializer: Materializer): ProtocolMessage = {
    implicit val ec = materializer.executionContext

    val parser = mkStreamingParser(strSource)
    val mapper = mkObjectMapper()

    // Lazily create a Message
    def mkMessage(maker: => ProtocolMessage) = try { maker } catch { case ex: Throwable => throw new DeserializeException(ex.getMessage, ex)}

    // Throw proper exception with message
    def fail(field: String) = throw new DeserializeException(s"Expected $field but ${parser.getCurrentToken} found")

    implicit class RichParser(parser: JsonParser) {
      def getValueAsDict(): Dict = {
        mapper.readValue(parser, classOf[Map[String, Map[_, _]]])
      }

      def getValueAsLazyPayload(): JsonTextLazyPayload = {
        // Firstly, gets the char count actually read
        // and consumed by the Jackson Streaming Parser
        val offset = parser.getCurrentLocation.getByteOffset.toInt
        /*
                               | unparsed
           strSource > ----------------------------- ...
                               |         |
                        parsed |         |
                       ~~~~~~~~~~~~~~~~~~,
              parser >  b u f f e r e d  |
                       ~~~~~~~~~~~~~~~~~~'
                               |
                             offset
         */
        // Thereafter, creates a custom Akka Stream Source which chops off
        // the first offset chars from the first emitted elements
        val unparsed = strSource.via(new ChopOff(offset))

        // Finally, returns the "lazy" payload which providing both
        // the chopped off unparsed Akka Stream Source
        // and the Jackson Streaming parser to let the user continue
        new JsonTextLazyPayload(unparsed, parser, mkObjectMapper())
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

      def |(p: Payload.type): Payload = parser.getValueAsLazyPayload
    }

    val message = 
      if (parser.nextToken() == START_ARRAY) {
        if (parser.nextToken() == VALUE_NUMBER_INT) {
          val tpe = parser.getIntValue
          tpe match {
            case Hello.tpe        => mkMessage(Hello("Realm"|Uri, "Details"|Dict))
            case Welcome.tpe      => mkMessage(Welcome("Session"|Id, "Details"|Dict))
            case Abort.tpe        => mkMessage(Abort("Details"|Dict, "Reason"|Uri))
            case Goodbye.tpe      => mkMessage(Goodbye("Details"|Dict, "Reason"|Uri))
            case Error.tpe        => mkMessage(Error("REQUEST.Type"|Int, "REQUEST.Request"|Id, "Details"|Dict, "Error"|Uri, "Arguments"|Payload))
            case Publish.tpe      => mkMessage(Publish("REQUEST.Request"|Id, "Details"|Dict, "Topic"|Uri, "Arguments"|Payload))
            case Published.tpe    => mkMessage(Published("PUBLISH.Request"|Id, "Publication"|Id))
            case Subscribe.tpe    => mkMessage(Subscribe("Request"|Id, "Options"|Dict, "Topic"|Uri))
            case Subscribed.tpe   => mkMessage(Subscribed("SUBSCRIBE.Request"|Id, "Subscription"|Id))
            case Unsubscribe.tpe  => mkMessage(Unsubscribe("Request"|Id, "SUBSCRIBE.Subscription"|Id))
            case Unsubscribed.tpe => mkMessage(Unsubscribed("UNSUBSCRIBE.Request"|Id))
            case Event.tpe        => mkMessage(Event("SUBSCRIBED.Subscription"|Id, "PUBLISHED.Publication"|Id, "Details"|Dict, "Arguments"|Payload))
            case Register.tpe     => mkMessage(Register("Request"|Id, "Options"|Dict, "Procedure"|Uri))
            case Registered.tpe   => mkMessage(Registered("REGISTER.Request"|Id, "Registration"|Id))
            case Unregister.tpe   => mkMessage(Unregister("Request"|Id, "REGISTER.Registration"|Id))
            case Unregistered.tpe => mkMessage(Unregistered("UNREGISTER.Request"|Id))
            case Call.tpe         => mkMessage(Call("Request"|Id, "Options"|Dict, "Procedure"|Uri, "Arguments"|Payload))
            case Invocation.tpe   => mkMessage(Invocation("Request"|Id, "REGISTERED.Registration"|Id, "Details"|Dict, "Arguments"|Payload))
            case Yield.tpe        => mkMessage(Yield("INVOCATION.Request"|Id, "Options"|Dict, "Arguments"|Payload))
            case Result.tpe       => mkMessage(Result("CALL.Request"|Id, "Details"|Dict, "YIELD.Arguments"|Payload))
            case _                => fail("MessageType|Integer")
          }
        }
        else fail("MessageType|Integer")
      }
      else fail("START_ARRAY")
    
    message
  }
  
  override def serialize(message: ProtocolMessage): Source[String, _] = {
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
        // when router serializes EVENT, INVOCATION and RESULT messages
        Source.single(fields.map(toJson).mkString("[", ",", "")).concat(p.unparsed)
        
      case Some(p: BinaryLazyPayload) => 
        throw new IllegalStateException("Cannot serialize binary payload to JSON")
        
      case Some(p: EagerPayload) =>
        // when client serializes messages
        val fieldsAndPayload = 
          if (p.args.isEmpty && p.kwargs.isEmpty) {
            fields
          }  
          else if (p.kwargs.isEmpty) {
            fields ::: p.args :: Nil
          }
          else {
            fields ::: p.args :: p.kwargs :: Nil
          }
        Source.single(fieldsAndPayload.map(toJson).mkString("[", ",", "]"))
    }
  }
}


