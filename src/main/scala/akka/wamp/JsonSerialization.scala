package akka.wamp

import akka.wamp.Messages._
import com.fasterxml.jackson.core.JsonToken._
import com.fasterxml.jackson.core._
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.module.scala._

import scala.util.Try


class JsonSerialization extends Serialization[String] {

  private val factory = new JsonFactory()
  private val mapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)
  
  
  def serialize(msg: Message): String = {
    def deep(any: Any): String = any match {
      case list: List[_] => list.map{ el => deep(el) }.mkString("[", ",", "]")
      case dict: Dict => dict.map { case (k,v) => s""""$k":${deep(v)}""" }.mkString("{",",","}")
      case str: String => s""""$str""""
      case null => null
      case any => any.toString
    }
    msg match {
      case Welcome(sessionId, details) => s"""[$WELCOME,$sessionId,${deep(details)}]"""
      case Goodbye(details, reason) => s"""[$GOODBYE,${deep(details)},"$reason"]"""
      case Published(requestId, publicationId) => s"""[$PUBLISHED,$requestId,$publicationId]"""
      case Event(subscriptionId, publicationId, details, arguments, argumentsKw) => s"""[$EVENT,$subscriptionId,$publicationId,${deep(details)},${deep(arguments)}""" + (if(argumentsKw.isDefined) s""",${deep(argumentsKw.get)}]""" else "]")
      case Subscribed(requestId, subscriptionId) => s"""[$SUBSCRIBED,$requestId,$subscriptionId]"""
      case Unsubscribed(requestId) => s"""[$UNSUBSCRIBED,$requestId]"""
      case Error(requestType, requestId, details, error) => s"""[$ERROR,$requestType,$requestId,${deep(details)},"$error"]"""
    }
  }

  
  // TODO couldn't we pass a textStream instead of a String ???
  def deserialize(text: String): Try[Message] = Try {
    // TODO factory.createParser(inputStream)
    val parser = factory.createParser(text)
    
    def fail(reason: String) = throw new JsonSerializingException(s"${reason} @${parser.getCurrentLocation.getLineNr}:${parser.getCurrentLocation.getColumnNr}")
    
    def build(builder: Builder) = try { builder.build() } catch { case ex: Exception => fail(ex.getMessage)}
    
    if (parser.nextToken() == START_ARRAY) {
      if (parser.nextToken() == VALUE_NUMBER_INT) {
        parser.getIntValue match {

          //[HELLO, Realm|uri, Details|dict]
          case HELLO => {
            val hello = new HelloBuilder
            if (parser.nextToken() == VALUE_STRING) {
              hello.realm = parser.getValueAsString
              if (parser.nextToken() == START_OBJECT) {
                hello.details = mapper.readValue(parser, classOf[Dict])
                parser.close()
                build(hello)
              }
              else fail("missing details dict") 
            }
            else fail("missing realm uri")
          }

          //[GOODBYE, Details|dict, Reason|uri]
          case GOODBYE => {
            val goodbye = new GoodbyeBuilder
            if (parser.nextToken() == START_OBJECT) {
              goodbye.details = mapper.readValue(parser, classOf[Dict])
              if (parser.nextToken() == VALUE_STRING) {
                goodbye.reason = parser.getValueAsString
              }
              // TODO are details and reason mandatory !?!
              else fail("missing reason uri")
            }
            else fail("missing details dict")
            parser.close()
            goodbye.build()
          }

          //[PUBLISH, Request|id, Options|dict, Topic|uri, Arguments|list, ArgumentsKw|dict]  
          case PUBLISH => {
            val publish = new PublishBuilder
            if (parser.nextToken() == VALUE_NUMBER_INT) {
              publish.requestId = parser.getValueAsLong
              if (parser.nextToken() == START_OBJECT) {
                publish.options = mapper.readValue(parser, classOf[Dict])
                if (parser.nextToken() == VALUE_STRING) {
                  publish.topic = parser.getValueAsString
                  if (parser.nextToken() == START_ARRAY) {
                    publish.arguments = mapper.readValue(parser, classOf[List[Any]])
                    if (parser.nextToken() == START_OBJECT) {
                      publish.argumentsKw = Some(mapper.readValue(parser, classOf[Dict]))
                    }
                    else publish.argumentsKw = None
                  }
                  else fail("missing arguments list")
                }
                else fail("missing topic uri")
              }
              else fail("missing options dict")
            }
            else fail("missing request id")
            parser.close()
            publish.build()
          }

          //[SUBSCRIBE, Request|id, Options|dict, Topic|uri]  
          case SUBSCRIBE => {
            val subscribe = new SubscribeBuilder
            if (parser.nextToken() == VALUE_NUMBER_INT) {
              subscribe.requestId = parser.getValueAsLong
              if (parser.nextToken() == START_OBJECT) {
                subscribe.options = mapper.readValue(parser, classOf[Dict])
                if (parser.nextToken() == VALUE_STRING) {
                  subscribe.topic = parser.getValueAsString
                }
                else fail("missing topic uri")
              }
              else fail("missing options dict") 
            }
            else fail("missing request id")
            parser.close()
            subscribe.build()
          }

          //[UNSUBSCRIBE, Request|id, Subscription|id]  
          case UNSUBSCRIBE => {
            val unsubscribe = new UnsubscribeBuilder
            if (parser.nextToken() == VALUE_NUMBER_INT) {
              unsubscribe.requestId = parser.getValueAsLong
              if (parser.nextToken() == VALUE_NUMBER_INT) {
                unsubscribe.subscriptionId = parser.getValueAsLong
              }
              else fail("missing subscription id")
            }
            else fail("missing request id")
            parser.close()
            unsubscribe.build()
          }

            
            
            
          case t => fail("invalid message type")
        }
      }
      else fail("missing message type")
    }
    else fail("missing start of array")
  }
}


class JsonSerializingException(message: String) extends Exception(message)