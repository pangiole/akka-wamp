package akka.wamp.serialization

import akka.wamp.Tpe._
import akka.wamp._
import akka.wamp.messages._
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.module.scala._
import org.slf4j.LoggerFactory

class JsonSerialization extends Serialization {

  type T = String

  private val log = LoggerFactory.getLogger(classOf[JsonSerialization])
  private val mapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)

  implicit class RichAny(any: Any) {
    def asInt = any.asInstanceOf[Int]

    def asString = any.asInstanceOf[String]

    def asDict = any.asInstanceOf[Dict]

    def asUri = any.asInstanceOf[Uri]

    def asSomePayload = any match {
      case map: Map[_, _] => Some(Payload(map.toList))
      case list: List[_] => Some(Payload(list))  
    }
    
    def asId = any match {
      case int: Int => int.toLong
      case long: Long => long
    }
  }

  def deserialize(text: String): Message = {
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
            case 6 => Error(requestType = arr(1).asInt, requestId = arr(2).asId, details = arr(3).asDict, error = arr(4).asUri, arr(5).asSomePayload)
            case 7 => Error(requestType = arr(1).asInt, requestId = arr(2).asId, details = arr(3).asDict, error = arr(4).asUri, arr(6).asSomePayload)
          }
        }
        case PUBLISH => {
          arr.length match {
            case 4 => Publish(requestId = arr(1).asId, topic = arr(3).asUri, options = arr(2).asDict)
            case 5 => Publish(requestId = arr(1).asId, topic = arr(3).asUri, arr(4).asSomePayload, options = arr(2).asDict)
            case 6 => Publish(requestId = arr(1).asId, topic = arr(3).asUri, arr(5).asSomePayload, options = arr(2).asDict)
          }
        }
        case PUBLISHED => {
          Published(
            requestId = arr(1).asId,
            publicationId = arr(2).asId
          )
        }
        case SUBSCRIBE => {
          Subscribe(requestId = arr(1).asId, topic = arr(3).asString, options = arr(2).asDict)
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
            case 4 => Event(subscriptionId = arr(1).asId, publicationId = arr(2).asId, details = arr(3).asDict)
            case 5 => Event(subscriptionId = arr(1).asId, publicationId = arr(2).asId, details = arr(3).asDict, arr(4).asSomePayload)
            case 6 => Event(subscriptionId = arr(1).asId, publicationId = arr(2).asId, details = arr(3).asDict, arr(5).asSomePayload)
          }
        }
      }
    } catch {
      case ex: Throwable =>
        throw new SerializationException(s"Bad message $text", ex)
    }
  }

  def serialize(msg: Message): String = {
    log.trace("Serializing {}", msg)

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

    val elems: List[Any] = msg match {
      case Hello(realm, details) =>
        List(HELLO, realm, details)

      case Welcome(session, details) =>
        List(WELCOME, session, details)

      case Goodbye(reason, details) =>
        List(GOODBYE, details, reason)

      case Abort(reason, details) =>
        List(ABORT, details, reason)

      case Error(requestType, requestId, details, error, None) =>
        List(ERROR, requestType, requestId, details, error)

      case Error(requestType, requestId, details, error, Some(payload)) =>
        List(ERROR, requestType, requestId, details, error) ++ payload.elems

      case Publish(requestId, topic, None, options) =>
        List(PUBLISH, requestId, options, topic)

      case Publish(requestId, topic, Some(payload), options) =>
        List(PUBLISH, requestId, options, topic) ++ payload.elems

      case Published(requestId, publicationId) =>
        List(PUBLISHED, requestId, publicationId)

      case Subscribe(requestId, topic, options) =>
        List(SUBSCRIBE, requestId, options, topic)

      case Subscribed(requestId, subscriptionId) =>
        List(SUBSCRIBED, requestId, subscriptionId)

      case Unsubscribe(requestId, subscriptionId) =>
        List(UNSUBSCRIBE, requestId, subscriptionId)

      case Unsubscribed(requestId) =>
        List(UNSUBSCRIBED, requestId)

      case Event(subscriptionId, publicationId, details, None) =>
        List(EVENT, subscriptionId, publicationId, details)

      case Event(subscriptionId, publicationId, details, Some(payload)) =>
        List(EVENT, subscriptionId, publicationId, details) ++ payload.elems
    }
    elems.map(toJson).mkString("[", ",", "]")
  }
}


