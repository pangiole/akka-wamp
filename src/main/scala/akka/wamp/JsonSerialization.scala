package akka.wamp

import akka.wamp.messages._
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonToken._
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule

import scala.util.Try


class JsonSerialization extends Serialization[String] {

  private val factory = new JsonFactory()
  val mapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)
  
  
  def serialize(msg: Message): String = {
    def ser(any: Any): String = any match {
      case dict: Dict => dict.map { case (k,v) => s""""$k":${ser(v)}""" }.mkString("{",",","}")
      case str: String => s""""$str""""
      case _ => "???"
      // TODO implement other serializing scenarios
    }
    msg match {
      case Welcome(sessionId, details) => s"""[$WELCOME,${sessionId},${ser(details)}]"""
      case Goodbye(details, reason) => s"""[$GOODBYE,${ser(details)},"${reason}"]"""
      case Subscribed(request, subscription) => s"""[$SUBSCRIBED,${request},${subscription}]"""
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

          //[SUBSCRIBE, Request|id, Options|dict, Topic|uri]  
          case SUBSCRIBE => {
            val subscribe = new SubscribeBuilder
            if (parser.nextToken() == VALUE_NUMBER_INT) {
              subscribe.request = parser.getValueAsLong
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
            
          case t => fail("invalid message type")
        }
      }
      else fail("missing message type")
    }
    else fail("missing start of array")
  }
}


class JsonSerializingException(message: String) extends Exception(message)