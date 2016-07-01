package akka.wamp.serialization


import akka.wamp.Wamp.Tpe._
import akka.wamp.Wamp._
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.module.scala._
import org.slf4j.LoggerFactory

class JsonSerialization extends Serialization[String] {
  
  private val log = LoggerFactory.getLogger(classOf[JsonSerialization])
  private val mapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)

  implicit class RichAny(any: Any) {
    def asInt = any.asInstanceOf[Int]
    def asString = any.asInstanceOf[String]
    def asDict = any.asInstanceOf[Dict]
    def asUri = any.asInstanceOf[Uri]
    def asListPayload = Some(Payload(any.asInstanceOf[List[Any]]))
    def asMapPayload = Some(Payload(any.asInstanceOf[Map[String, Any]]))
    def asId = any match {
      case int: Int => int.toLong
      case long: Long => long
    }
  }
  
  def deserialize(text: String): WampMessage = {
    log.trace("Deserializing {}", text)
    try {
      val arr = mapper.readValue(text, classOf[Array[Any]])
      arr(0) match {
        case HELLO => {
          Hello(
            realm = arr(1).asString,
            details = arr(2).asDict
          )
        }
        case WELCOME => {
          Welcome(
            sessionId = arr(1).asId,
            details = arr(2).asDict
          )
        }
        case ABORT => {
          Abort(
            details = arr(1).asDict,
            reason = arr(2).asUri
          )
        }
        case GOODBYE => {
          Goodbye(
            details = arr(1).asDict,
            reason = arr(2).asUri
          )
        }
        case ERROR => {
          arr.length match {
            case 5 => Error(requestType = arr(1).asInt, requestId = arr(2).asId, details = arr(3).asDict, error = arr(4).asUri)
            case 6 => Error(requestType = arr(1).asInt, requestId = arr(2).asId, details = arr(3).asDict, error = arr(4).asUri, arr(5).asListPayload)
            case 7 => Error(requestType = arr(1).asInt, requestId = arr(2).asId, details = arr(3).asDict, error = arr(4).asUri, arr(6).asMapPayload)
          }
        }
        case PUBLISH =>  {
          arr.length match {
            case 4 => Publish(requestId = arr(1).asInt, options = arr(2).asDict, topic = arr(3).asUri)
            case 5 => Publish(requestId = arr(1).asInt, options = arr(2).asDict, topic = arr(3).asUri, arr(4).asListPayload)
            case 6 => Publish(requestId = arr(1).asInt, options = arr(2).asDict, topic = arr(3).asUri, arr(5).asMapPayload)
          }
        }
        case PUBLISHED => {
          Published(
            requestId = arr(1).asId,
            publicationId = arr(2).asId
          )
        }
        case SUBSCRIBE => {
          Subscribe(
            requestId = arr(1).asId,
            options = arr(2).asDict,
            topic = arr(3).asString
          )
        }
        case SUBSCRIBED => {
          Subscribed(
            requestId = arr(1).asId,
            subscriptionId = arr(2).asId
          )
        }
        case UNSUBSCRIBE => {
          Unsubscribe(
            requestId = arr(1).asId,
            subscriptionId = arr(2).asId
          )
        }
        case UNSUBSCRIBED => {
          Unsubscribed(
            requestId = arr(1).asId
          )
        }
        case EVENT => {
          arr.length match {
            case 4 => Event(subscriptionId = arr(1).asInt, publicationId = arr(2).asInt, details = arr(3).asDict)
            case 5 => Event(subscriptionId = arr(1).asInt, publicationId = arr(2).asInt, details = arr(3).asDict, arr(4).asListPayload)
            case 6 => Event(subscriptionId = arr(1).asInt, publicationId = arr(2).asInt, details = arr(3).asDict, arr(5).asMapPayload)
          }
        }
      }
    } catch {
      case ex: Throwable =>
        throw new SerializingException(s"Bad message $text", ex)
    }
  }
  
  def serialize(msg: WampMessage): String = {
    log.trace("Serializing {}", msg)
    
    def toJson(any: Any): String = any match {
      case Some(v) => toJson(v)
      case Left(args) => toJson(args)
      case list: List[_] => list.map(toJson).mkString("[", ",", "]")
      case Right(argsKw) => toJson(argsKw)
      case dict: Map[_, _] => dict.map { case (k,v) => s""""$k":${toJson(v)}""" }.mkString("{",",","}")
      case str: String => s""""$str""""
      case None | null => null
      case any => any.toString
    }
    
    def prep(payload: Payload, fields: List[Any]): List[Any] = {
      payload match {
        case Payload(Left(args)) => args :: fields
        case Payload(Right(args)) => Nil :: args :: fields
      }
    }
    var fields = List.empty[Any]
    msg match {
      case Hello(realm, details) => 
        fields = HELLO :: realm :: details :: fields
      
      case Welcome(session, details) => 
        fields = WELCOME :: session :: details :: fields
      
      case Goodbye(details, reason) => 
        fields = GOODBYE :: details :: reason :: fields
      
      case Abort(details, reason) =>
        fields = ABORT :: details :: reason :: fields
      
      case Error(requestType, requestId, details, error, None) => 
        fields = ERROR :: requestType :: requestId :: details :: error :: fields
      
      case Error(requestType, requestId, details, error, Some(payload)) => 
        fields = ERROR :: requestType :: requestId :: details :: error :: prep(payload, fields)
       
      case Publish(requestId, options, topic, None) => 
        fields = PUBLISH :: requestId :: options :: topic :: fields
      
      case Publish(requestId, options, topic, Some(payload)) => 
        fields = PUBLISH :: requestId :: options :: topic :: fields :: prep(payload, fields)
      
      case Published(requestId, publicationId) => 
        fields = PUBLISHED :: requestId :: publicationId :: fields
      
      case Subscribe(requestId, options, topic) => 
        fields = SUBSCRIBE :: requestId :: options :: topic :: fields
      
      case Subscribed(requestId, subscriptionId) => 
        fields = SUBSCRIBED :: requestId :: subscriptionId :: fields
      
      case Unsubscribe(requestId, subscriptionId) => 
        fields = UNSUBSCRIBE :: requestId :: subscriptionId :: fields
      
      case Unsubscribed(requestId) => 
        fields = UNSUBSCRIBED :: requestId :: fields
      
      case Event(subscriptionId, publicationId, details, None) => 
        fields = EVENT :: subscriptionId :: publicationId :: details :: fields
      
      case Event(subscriptionId, publicationId, details, Some(payload)) => 
        fields = EVENT :: subscriptionId :: publicationId :: details :: prep(payload, fields)
    }
    fields.map(toJson).mkString("[", ",", "]")
  }

  
  
}


