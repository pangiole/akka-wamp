package akka.wamp.serializations

import akka.wamp.messages.HelloMessageBuilder
import akka.wamp.{Message, MessageBuilder, ProtocolError, Serialization}
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonToken._
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule

import scala.util.Try


object JsonSerialization extends Serialization[String] {

  private val factory = new JsonFactory()
  val mapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)
  
  
  def serialize(message: Message): String = ???

  
  def deserialize(text: String): Try[Message] = Try {
    def fail() = throw new ProtocolError(BadMessage)
    def build(builder: MessageBuilder) = try { builder.build() } catch { case ex: Throwable => throw new ProtocolError(BadMessage, ex) }
    
    val parser = factory.createParser(text)
    
    if (parser.nextToken() == START_ARRAY) {
      if (parser.nextToken() == VALUE_NUMBER_INT) {
        val code = parser.getIntValue
        code match {
          
          // HELLO
          case 1 => {
            val hello = new HelloMessageBuilder()
            if (parser.nextToken() == VALUE_STRING) {
              hello.realm = parser.getValueAsString
              if (parser.nextToken() == START_OBJECT) {
                hello.details = mapper.readValue(parser, classOf[Map[String, Map[_, _]]])
                parser.close()
                build(hello)  
              }
              else fail()    
            } 
            else fail()
          }
            
          case _ => throw new ProtocolError(s"Unknown message code $code")
        }
      } 
      else fail()
    }
    else fail()
  }
  
  
  private val BadMessage = "Bad message"
}
