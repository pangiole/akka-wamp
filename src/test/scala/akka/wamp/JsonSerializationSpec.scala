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

          """[6,null]""",
          """[6,{}]""",
          """[6,{},null]""",
          
          """[32,null]""",
          """[32,713845233,null]""",
          """[32,713845233,{},null]""",
          
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

      "deserialize GOODBYE" in {
        val m = s.deserialize("""  [  6  ,  {"message": "The host is shutting down now."},  "wamp.error.system_shutdown"] """)
        m.success.value mustBe a[Goodbye]
        val goodbye = m.success.value.asInstanceOf[Goodbye]
        goodbye.details mustBe Map("message" -> "The host is shutting down now.")
        goodbye.reason mustBe "wamp.error.system_shutdown"
      }

      "deserialize SUBSCRIBE" in {
        val m = s.deserialize("""[32, 713845233, {}, "com.myapp.mytopic1"]""")
        m.success.value mustBe a[Subscribe]
        val subscribe = m.success.value.asInstanceOf[Subscribe]
        subscribe.request mustBe 713845233L
        subscribe.options must have size(0)
        subscribe.topic mustBe "com.myapp.mytopic1"
      }
    }
    
    "serializing from Message to JSON" should {
      
      "serialize WELCOME" in {
        val msg = Welcome(123L, DictBuilder().withEntry("agent", "akka-wamp-0.1.0").withRoles(Set("broker")).build())
        val json = s.serialize(msg)
        json mustBe """[2,123,{"agent":"akka-wamp-0.1.0","roles":{"broker":{}}}]"""
      }

      "serialize GOODBYE" in {
        val msg = Goodbye(DictBuilder().build(), "wamp.error.goodbye_and_out")
        val json = s.serialize(msg)
        json mustBe """[6,{},"wamp.error.goodbye_and_out"]"""
      }

      "serialize SUBSCRIBED" in {
        val msg = Subscribed(713845233L, 5512315355L)
        val json = s.serialize(msg)
        json mustBe """[33,713845233,5512315355]"""
      }
    }
  }
  
}
