package akka.wamp.serialization

import akka.wamp.Tpe._
import akka.wamp._
import akka.wamp.{messages => wamp}
import org.scalatest._

class JsonSerializationSpec extends WordSpec 
  with MustMatchers with TryValues with OptionValues with EitherValues with ParallelTestExecution {
  
  val s = new JsonSerialization
  
  "The wamp.2.json serialization" when {
    "deserializing" should {
      
      "fail for invalid messages" in {
        List(
          """  { noscan """,
          """["invalid",noscan] """,
          """[null,""",
          """[999,noscan] """
        ).foreach { text =>
          a[DeserializeException] mustBe thrownBy(s.deserialize(text))
        }
      }
      
      //[HELLO, Realm|uri, Details|dict]
      "fail for invalid HELLO" in {
        List(
          """[1]""",
          """[1,null]""",
          """[1,"myapp.realm"]""",
          """[1,"myapp.realm",null]""",
          """[1,"myapp.realm",{}]"""",
          """[1,"myapp.realm",{"roles":null}]"""",
          """[1,"myapp.realm",{"roles":{}}]"""",
          """[1,"myapp.realm",{"roles":{"unknown":{}}}]"""",
          """[1,"invalid!realm",{"roles":{"publisher":{}}}]""""
        ).foreach { text =>
          a[DeserializeException] mustBe thrownBy(s.deserialize(text))
        }
      }
      "succeed for valid HELLO" in {
        s.deserialize("""[  1  ,"myapp.realm",  {"roles":{"caller":{},"callee":{}}}]""") match {
          case m: wamp.Hello =>
            m.realm mustBe "myapp.realm"
            m.details mustBe Map("roles" -> Map("caller" -> Map(), "callee" -> Map()))
          case _ => fail
        }
      }

      //[WELCOME, Session|id, Details|dict]
      "fail for invalid WELCOME" in {
        List(
          """[2]""",
          """[2,null]""",
          """[2,1]""",
          """[2,1,null]""",
          """[2,0,{"roles":{"broker":{}}}]""",
          """[2,9007199254740993,{"roles":{"broker":{}}}]"""
        ).foreach { text =>
          a[DeserializeException] mustBe thrownBy(s.deserialize(text))
        }
      }
      "succeed for valid WELCOME" in {
        s.deserialize("""[2,9007199254740992,{"roles":{"broker":{}}}]""") match {
          case m: wamp.Welcome =>
            m.sessionId mustBe 9007199254740992L
            m.details mustBe Map("roles" -> Map("broker" -> Map()))
          case _ => fail
        }
      }

      
      //[ABORT, Details|dict, Reason|uri]
      "fail for invalid ABORT" in {
        List(
          """[3]""",
          """[3,null]""",
          """[3,{}]""",
          """[3,{},null]"""
        ).foreach { text =>
          a[DeserializeException] mustBe thrownBy(s.deserialize(text))
        }
      }
      "succeed for valid ABORT" in {
        s.deserialize("""[3, {"message": "The realm does not exist."},"wamp.error.no_such_realm"]""") match {
          case m: wamp.Abort =>
            m.details mustBe Map("message" ->"The realm does not exist.")
            m.reason mustBe "wamp.error.no_such_realm"
          case _ => fail
        }
      }

      
      //[GOODBYE, Details|dict, Reason|uri]
      "fail for invalid GOODBYE" in {
        List(
          """[6]""",
          """[6,null]""",
          """[6,{}]""",
          """[6,{},null]""",
          """[6,{},"invalid!uri"]"""
        ).foreach { text =>
          a[DeserializeException] mustBe thrownBy(s.deserialize(text))
        }
      }
      "succeed for valid GOODBYE" in {
        s.deserialize("""[6,{"message": "The host is shutting down now."},"system_shutdown"]""") match {
          case m: wamp.Goodbye =>
            m.details mustBe Map("message" -> "The host is shutting down now.")
            m.reason mustBe "system_shutdown"
          case _ => fail
        }
      }

      
      //[ERROR, REQUEST.Type|int, REQUEST.Request|id, Details|dict, Error|uri]
      //[ERROR, REQUEST.Type|int, REQUEST.Request|id, Details|dict, Error|uri, Arguments|list]
      //[ERROR, REQUEST.Type|int, REQUEST.Request|id, Details|dict, Error|uri, Arguments|list, ArgumentsKw|dict]
      "fail for invalid ERROR" in {
        List(
          """[8]""",
          """[8,null]""",
          """[8,34,null]""",
          """[8,34,1,null]""",
          """[8,34,1,{},null]""",
          """[8,34,1,{},"invalid!error"]""",
          """[8,34,1,{},"wamp.error.no_such_subscription",null]""",
          """[8,34,1,{},"wamp.error.no_such_subscription",[],null]""",
          """[8,99,1,{},"wamp.error.no_such_subscription"]""",
          """[8,34,0,{},"wamp.error.no_such_subscription"]""",
          """[8,34,9007199254740993,{},"wamp.error.no_such_subscription"]"""
        ).foreach { text =>
          a[DeserializeException] mustBe thrownBy(s.deserialize(text))
        }
      }
      "succeed for valid ERROR without payload" in {
        s.deserialize("""[8,34,9007199254740992,{},"wamp.error.no_such_subscription"]""") match {
          case m: wamp.Error =>
            m.requestType mustBe 34
            m.requestId mustBe 9007199254740992L
            m.details mustBe empty
            m.error mustBe "wamp.error.no_such_subscription"
            m.payload mustBe None
          case _ => fail
        }
      }
      "succeed for valid ERROR with payload as list" in {
        s.deserialize(s"""[8,34,1,{},"wamp.error.no_such_subscription",["paolo",40,true]]""") match {
          case m: wamp.Error =>
            m.requestType mustBe 34
            m.requestId mustBe 1
            m.details mustBe empty
            m.error mustBe "wamp.error.no_such_subscription"
            m.payload.value.arguments mustBe List("paolo", 40, true)
          case _ => fail
        }
      }
      "succeed for valid ERROR with payload as map" in {
        s.deserialize(s"""[8,34,1,{},"wamp.error.no_such_subscription",[],{"arg0":"paolo","age":40,"arg2":true}]""") match {
          case m: wamp.Error =>
            m.requestType mustBe 34
            m.requestId mustBe 1
            m.details mustBe empty
            m.error mustBe "wamp.error.no_such_subscription"
            m.payload.value.arguments mustBe List("arg0"->"paolo", "age"->40, "arg2"->true)
          case _ => fail
        }
      }


      //[PUBLISH, Request|id, Options|dict, Topic|uri]
      //[PUBLISH, Request|id, Options|dict, Topic|uri, Arguments|list]
      //[PUBLISH, Request|id, Options|dict, Topic|uri, Arguments|list, ArgumentsKw|dict]
      "fail for invalid PUBLISH" in {
        List(
          """[16]""",
          """[16,null]""",
          """[16,1,null]""",
          """[16,1,{},null]""",
          """[16,1,{},"invalid!topic"]""",
          """[16,1,{},"myapp.topic1",null]""",
          """[16,1,{},"myapp.topic1",[],null]""",
          """[16,9007199254740993,{},"myapp.topic1",[],null]""",
          """[16,0,{},"myapp.topic1",[],null]"""
        ).foreach { text =>
          a[DeserializeException] mustBe thrownBy(s.deserialize(text))
        }
      }
      "succeed for valid PUBLISH without payload" in {
        s.deserialize("""[16,9007199254740992,{"acknowledge":true},"myapp.topic1"]"""") match {
          case m: wamp.Publish =>
            m.requestId mustBe 9007199254740992L
            m.options mustBe Map("acknowledge"->true)
            m.topic mustBe "myapp.topic1"
            m.payload mustBe None
          case _ =>
            fail("type mismatch")
        }
      }
      "succeed for valid PUBLISH with payload as list" in {
        s.deserialize(s"""[16,1,{},"myapp.topic1",["paolo",40,true]]""") match {
          case m: wamp.Publish =>
            m.requestId mustBe 1
            m.options mustBe empty
            m.topic mustBe "myapp.topic1"
            m.payload.value.arguments mustBe List("paolo", 40, true)
          case _ => fail
        }
      }
      "succeed for valid PUBLISH with payload as map" in {
        s.deserialize(s"""[16,1,{},"myapp.topic1",[],{"arg0":"paolo","age":40,"arg2":true}]""") match {
          case m: wamp.Publish =>
            m.requestId mustBe 1
            m.options mustBe empty
            m.topic mustBe "myapp.topic1"
            m.payload.value.arguments mustBe List("arg0"->"paolo", "age"->40, "arg2"->true)
          case _ => fail
        }
      }

      
      
      //[PUBLISHED, PUBLISH.Request|id, Publication|id]
      "fail for invalid PUBLISHED" in {
        List(
          """[17]""",
          """[17,null]""",
          """[17,1]""",
          """[17,1,null]""",
          """[17,0,2]""",
          """[17,9007199254740993,2]""",
          """[17,1,0]""",
          """[17,1,9007199254740993]"""
        ).foreach { text =>
          a[DeserializeException] mustBe thrownBy(s.deserialize(text))
        }
      }
      "succeed for valid PUBLISHED" in {
        s.deserialize("""[17,9007199254740988,9007199254740992]""") match {
          case m: wamp.Published =>
            m.requestId mustBe 9007199254740988L
            m.publicationId mustBe 9007199254740992L
          case _ => fail
        }
      }


      //[SUBSCRIBE, Request|id, Options|dict, Topic|uri]
      "fail for invalid SUBSCRIBE" in {
        List(
          """[32]""",
          """[32,null]""",
          """[32,1,null]""",
          """[32,1,{},null]""",
          """[32,1,{},"invalid!uri"]""",
          """[32,0,{},"myapp.topic1"]""",
          """[32,9007199254740993,{},"myapp.topic1"]"""
        ).foreach { text =>
          a[DeserializeException] mustBe thrownBy(s.deserialize(text))
        }
      }
      "succeed for valid SUBSCRIBE" in {
        s.deserialize("""[32,9007199254740992,{},"myapp.topic1"]""") match {
          case m: wamp.Subscribe =>
            m.requestId mustBe 9007199254740992L
            m.options mustBe empty
            m.topic mustBe "myapp.topic1"
          case _ => fail
        }
      }


      //[SUBSCRIBED, SUBSCRIBE.Request|id, Subscription|id]
      "fail for invalid SUBSCRIBED" in {
        List(
          """[33]""",
          """[33,null]""",
          """[33,1]""",
          """[33,1,null]""",
          """[33,0,2]""",
          """[33,9007199254740993,2]""",
          """[33,1,0]""",
          """[33,1,9007199254740993]"""
        ).foreach { text =>
          a[DeserializeException] mustBe thrownBy(s.deserialize(text))
        }
      }
      "succeed for valid SUBSCRIBED" in {
        s.deserialize("""[33,9007199254740977,9007199254740992]""") match {
          case m: wamp.Subscribed =>
            m.requestId mustBe 9007199254740977L
            m.subscriptionId mustBe 9007199254740992L
          case _ => fail
        }
      }

      
      //[UNSUBSCRIBE, Request|id, SUBSCRIBED.Subscription|id]
      "fail for invalid UNSUBSCRIBE" in {
        List(
          """[34]""",
          """[34,null]""",
          """[34,1]""",
          """[34,1,null]""",
          """[34,0,2]""",
          """[34,9007199254740993,2]""",
          """[34,1,0]""",
          """[34,1,9007199254740993]"""
        ).foreach { text =>
          a[DeserializeException] mustBe thrownBy(s.deserialize(text))
        }
      }
      "succeed for valid UNSUBSCRIBE" in {
        s.deserialize("""[34,9007199254740955,9007199254740992]""") match {
          case m: wamp.Unsubscribe =>
            m.requestId mustBe 9007199254740955L
            m.subscriptionId mustBe 9007199254740992L
          case _ => fail
        }
      }


      //[UNSUBSCRIBED, UNSUBSCRIBE.Request|id]
      "fail for invalid UNSUBSCRIBED" in {
        List(
          """[35]""",
          """[35,null]""",
          """[35,0]""",
          """[35,9007199254740993]"""
        ).foreach { text =>
          a[DeserializeException] mustBe thrownBy(s.deserialize(text))
        }
      }
      "succeed for valid UNSUBSCRIBED" in {
        s.deserialize("""[35,9007199254740992]""") match {
          case m: wamp.Unsubscribed => 
            m.requestId mustBe 9007199254740992L
          case _ =>  fail
        }
      }

      
      //[EVENT, SUBSCRIBED.Subscription|id, PUBLISHED.Publication|id, Details|dict]
      //[EVENT, SUBSCRIBED.Subscription|id, PUBLISHED.Publication|id, Details|dict, Arguments|list]
      //[EVENT, SUBSCRIBED.Subscription|id, PUBLISHED.Publication|id, Details|dict, Arguments|list, ArgumentsKw|dict]
      "fail for invalid EVENT" in {
        List(
          """[36]""",
          """[36,null]""",
          """[36,1,null]""",
          """[36,1,2,null]""",
          """[36,1,2,{},null]""",
          """[36,1,2,{},[],null]""",
          """[36,0,2,{}]""",
          """[36,9007199254740993,2,{}]""",
          """[36,1,0,{}]""",
          """[36,1,9007199254740993,{}]"""
        ).foreach { text =>
          a[DeserializeException] mustBe thrownBy(s.deserialize(text))
        }
      }
      
      "succeed for valid EVENT without payload" in {
        s.deserialize("""[36,9007199254740933,9007199254740992,{}]""") match {
          case m: wamp.Event =>
            m.subscriptionId mustBe 9007199254740933L
            m.publicationId mustBe 9007199254740992L
            m.details mustBe empty
            m.payload mustBe None
          case _ => fail
        }
      }
      
      "succeed for valid EVENT with payload as list" in {
        s.deserialize(s"""[36,1,2,{},["paolo",40,true]]""") match {
          case m: wamp.Event =>
            m.subscriptionId mustBe 1
            m.publicationId mustBe 2
            m.details mustBe empty
            m.payload.value.arguments mustBe List("paolo", 40, true)
          case _ => fail
        }
      }
      
      "succeed for valid EVENT with payload as map" in {
        s.deserialize(s"""[36,1,2,{},[],{"arg0":"paolo","age":40,"arg2":true}]""") match {
          case m: wamp.Event =>
            m.subscriptionId mustBe 1
            m.publicationId mustBe 2
            m.details mustBe empty
            m.payload.value.arguments mustBe List("arg0"->"paolo", "age"->40, "arg2"->true)
          case _ => fail
        }
      }
    }
    
    
    
    "serializing" should {

      "serialize HELLO" in {
        val msg = wamp.Hello("akka.wamp.realm", Dict().withRoles("publisher"))
        val json = s.serialize(msg)
        json mustBe """[1,"akka.wamp.realm",{"roles":{"publisher":{}}}]"""
      }
      
      "serialize WELCOME" in {
        val msg = wamp.Welcome(1233242, Dict().withAgent("akka-wamp-0.5.1").withRoles("broker"))
        val json = s.serialize(msg)
        json mustBe """[2,1233242,{"agent":"akka-wamp-0.5.1","roles":{"broker":{}}}]"""
      }

      "serialize GOODBYE" in {
        val msg = wamp.Goodbye("wamp.error.goobye_and_out", Dict())
        val json = s.serialize(msg)
        json mustBe """[6,{},"wamp.error.goobye_and_out"]"""
      }

      "serialize ABORT" in {
        pending
      }

      "serialize ERROR" in {
        val msg1 = wamp.Error(SUBSCRIBE, 341284, Dict(), "wamp.error.no_such_subscription")
        val json1 = s.serialize(msg1)
        json1 mustBe """[8,32,341284,{},"wamp.error.no_such_subscription"]"""

        val msg2 = wamp.Error(SUBSCRIBE, 341284, Dict(), "wamp.error.no_such_subscription", Some(Payload("paolo", 40, true)))
        val json2 = s.serialize(msg2)
        json2 mustBe s"""[8,32,341284,{},"wamp.error.no_such_subscription",["paolo",40,true]]"""

        val msg3 = wamp.Error(SUBSCRIBE, 341284, Dict(), "wamp.error.no_such_subscription", Some(Payload("paolo", "age"->40, true)))
        val json3 = s.serialize(msg3)
        json3 mustBe s"""[8,32,341284,{},"wamp.error.no_such_subscription",[],{"arg0":"paolo","age":40,"arg2":true}]"""
      }


      "serialize PUBLISH" in {
        val msg1 = wamp.Publish(341284, "myapp.topic1")
        val json1 = s.serialize(msg1)
        json1 mustBe """[16,341284,{},"myapp.topic1"]"""

        val msg2 = wamp.Publish(341284, "myapp.topic1", Some(Payload("paolo", 40, true)))
        val json2 = s.serialize(msg2)
        json2 mustBe """[16,341284,{},"myapp.topic1",["paolo",40,true]]"""

        val msg3 = wamp.Publish(341284, "myapp.topic1", Some(Payload("paolo", "age"->40, true)))
        val json3 = s.serialize(msg3)
        json3 mustBe """[16,341284,{},"myapp.topic1",[],{"arg0":"paolo","age":40,"arg2":true}]"""
      }

      "serialize PUBLISHED" in {
        val msg = wamp.Published(713845233, 5512315)
        val json = s.serialize(msg)
        json mustBe """[17,713845233,5512315]"""
      }
      
      "serialize SUBSCRIBE" in {
        val msg = wamp.Subscribe(1, "myapp.topic1", Dict())
        val json = s.serialize(msg)
        json mustBe """[32,1,{},"myapp.topic1"]"""
      }
      
      "serialize SUBSCRIBED" in {
        val msg = wamp.Subscribed(713845233, 5512315)
        val json = s.serialize(msg)
        json mustBe """[33,713845233,5512315]"""
      }

      "serialize UNSUBSCRIBED" in {
        val msg = wamp.Unsubscribed(85346237)
        val json = s.serialize(msg)
        json mustBe """[35,85346237]"""
      }

      "serialize EVENT" in {
        val msg1 = wamp.Event(713845233, 5512315, Dict())
        val json1 = s.serialize(msg1)
        json1 mustBe """[36,713845233,5512315,{}]"""
        
        val msg2 = wamp.Event(713845233, 5512315, Dict(), Some(Payload("paolo", 40, true)))
        val json2 = s.serialize(msg2)
        json2 mustBe s"""[36,713845233,5512315,{},["paolo",40,true]]"""

        val msg3 = wamp.Event(713845233, 5512315, Dict(), Some(Payload("paolo", "age"->40, true)))
        val json3 = s.serialize(msg3)
        json3 mustBe s"""[36,713845233,5512315,{},[],{"arg0":"paolo","age":40,"arg2":true}]"""
      }
    }
  }
}
