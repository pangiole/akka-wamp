package akka.wamp

import akka.wamp.messages._
import org.scalatest._


class JsonSerializationSpec extends WordSpec with MustMatchers with TryValues {
  val s = new JsonSerialization
  
  "The wamp.2.json serialization" when {
    "deserializing from JSON to Message" should {

      "fail for bad messages" in {
        Seq(
          """  { noscan """,
          """["invalid",noscan] """,
          """[null,""",
          """[1,null]""",
          """[1,"some.realm",null]""",
          """[1,"some.realm",{}]""",
          """[1,"some.realm",{"roles":null}]""",
          """[1,"some.realm",{"roles":{}}]""",
          """[1,"some.realm",{"roles":{"unknown":{}}}]""",
          """[999,noscan] """
          
        ).foreach { json =>
          val ex = s.deserialize(json).failure.exception
          ex mustBe a[JsonSerializingException]
        }
      }
      
      "deserialize HELLO" in {
        val m = s.deserialize("""   [  1  ,"test.realm.uri",  {"roles":{"caller":{},"callee":{}}}] """)
        m.success.value mustBe an[Hello]
        val hello = m.success.value.asInstanceOf[Hello]
        hello.realm mustBe "test.realm.uri"
        hello.details mustBe Map("roles" -> Map("caller" -> Map(), "callee" -> Map()))
      }
    }
    
    "serializing from Message to JSON" should {
      
      "serialize WELCOME" in {
        val message = Welcome(123L, Map("roles" -> Map("broker" -> Map())))
        val json = s.serialize(message)
        json mustBe """[2,123,{"roles":{"broker":{}}}]"""
      }
    }
  }
  
}
