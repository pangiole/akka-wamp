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
      case m: Map[_,_] => m.map { case (k,v) => s""""$k":${ser(v)}""" }.mkString("{",",","}")
      case _ => "???"
      // TODO implement other serializing scenarios
    }
    msg match {
      case Welcome(sessionId, details) => s"""[$WELCOME,${sessionId},${ser(details)}]"""
      case Goodbye(details, reason) => s"""[$GOODBYE,${ser(details)},"${reason}"]"""
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

          case HELLO => {
            val hello = new HelloBuilder()
            if (parser.nextToken() == VALUE_STRING) {
              hello.realm = parser.getValueAsString
              if (parser.nextToken() == START_OBJECT) {
                hello.details = mapper.readValue(parser, classOf[Dict])
                parser.close()
                build(hello)
              }
              else fail("missing start of object") 
            }
            else fail("missing realm uri")
          }

          case GOODBYE => {
            val goodbye = new GoodbyeBuilder()
            if (parser.nextToken() == START_OBJECT) {
              goodbye.details = mapper.readValue(parser, classOf[Dict])
              if (parser.nextToken() == VALUE_STRING) {
                goodbye.reason = parser.getValueAsString
              }
              else fail("missing reason uri")
            }
            else fail("missing start of object")
            parser.close()
            goodbye.build()
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