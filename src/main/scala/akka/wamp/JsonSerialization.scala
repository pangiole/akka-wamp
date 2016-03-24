package akka.wamp

import akka.wamp.messages._
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonToken._
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import jdk.nashorn.internal.parser.JSONParser

import scala.util.Try


class JsonSerialization extends Serialization[String] {

  private val factory = new JsonFactory()
  val mapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)
  
  
  def serialize(message: Message): String = {
    def ser(any: Any): String = any match {
      case m: Map[_,_] => m.map { case (k,v) => s""""$k":${ser(v)}""" }.mkString("{",",","}")
      case _ => "???"
      // TODO implement other serializing scenarios
    }
    message match {
      case Welcome(sessionId, details) =>
        s"""[$WELCOME,${sessionId},${ser(details)}]"""
    }
  }

  
  def deserialize(text: String): Try[Message] = Try {
    val parser = factory.createParser(text)
    
    def fail(reason: String) = throw new JsonSerializingException(s"${reason} @${parser.getCurrentLocation.getLineNr}:${parser.getCurrentLocation.getColumnNr}")
    
    def build(builder: MessageBuilder) = try { builder.build() } catch { case ex: Exception => fail(ex.getMessage)}
    
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
              // no START_OBJECT for details
              else fail("missing start of object")    
            }
            // no VALUE_STRING for realm  
            else fail("missing realm uri")
          }
          case t => fail("invalid message type")
        }
      }
      // no VALUE_NUMBER_INT for message type  
      else fail("missing message type")
    }
    // no START_ARRAY
    else fail("missing start of array")
  }
}


class JsonSerializingException(message: String) extends Exception(message)