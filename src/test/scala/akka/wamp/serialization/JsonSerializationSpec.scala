package akka.wamp.serialization

import akka.wamp.Wamp.Tpe._
import akka.wamp.Wamp._
import akka.wamp._
import org.scalatest._

class JsonSerializationSpec extends WordSpec 
  with MustMatchers with TryValues with OptionValues with EitherValues {
  
  val s = new JsonSerialization
  
  "The wamp.2.json serialization" when {
    "deserializing from JSON" should {
      
      "fail for invalid messages" in {
        List(
          """  { noscan """,
          """["invalid",noscan] """,
          """[null,""",
          """[999,noscan] """
        ).foreach { text =>
          a[SerializingException] mustBe thrownBy(s.deserialize(text))
        }
      }
      
      //[HELLO, Realm|uri, Details|dict]
      "fail for invalid HELLO" in {
        List(
          """[1]""",
          """[1,null]""",
          """[1,"akka.wamp.realm"]""",
          """[1,"akka.wamp.realm",null]""",
          """[1,"invalid !",{}]""",
          """[1,"akka.wamp.realm",{}]"""",
          """[1,"akka.wamp.realm",{"roles":null}]"""",
          """[1,"akka.wamp.realm",{"roles":{}}]"""",
          """[1,"akka.wamp.realm",{"roles":{"unknown":{}}}]""""
        ).foreach { text =>
          a[SerializingException] mustBe thrownBy(s.deserialize(text))
        }
      }
      "succeed for valid HELLO" in {
        s.deserialize("""[  1  ,"test.realm.uri",  {"roles":{"caller":{},"callee":{}}}]""") match {
          case m: Hello =>
            m.realm mustBe "test.realm.uri"
            m.details mustBe Map("roles" -> Map("caller" -> Map(), "callee" -> Map()))
          case _ => fail
        }
      }

      //[WELCOME, Session|id, Details|dict]
      "fail for invalid WELCOME" in {
        List(
          """[2]""",
          """[2,null]""",
          """[2,1431253]""",
          """[2,1431253,null]""",
          """[2,421341248931241243312432434,{}]""",
          """[2,9223372036854775807,{}]"""
        ).foreach { text =>
          a[SerializingException] mustBe thrownBy(s.deserialize(text))
        }
      }
      "succeed for valid WELCOME" in {
        s.deserialize("""[2,1,{"roles":{"broker":{}}}]""") match {
          case m: Welcome =>
            m.sessionId mustBe 1
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
          a[SerializingException] mustBe thrownBy(s.deserialize(text))
        }
      }
      "succeed for valid ABORT" in {
        s.deserialize("""[3, {"message": "The realm does not exist."},"wamp.error.no_such_realm"]""") match {
          case m: Abort =>
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
          """[6,{},"invalid uri"]"""
        ).foreach { text =>
          a[SerializingException] mustBe thrownBy(s.deserialize(text))
        }
      }
      "succeed for valid GOODBYE" in {
        s.deserialize("""[6,{"message": "The host is shutting down now."},"system_shutdown"]""") match {
          case m: Goodbye =>
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
          """[8,34,321443,null]""",
          """[8,34,321443,{},null]""",
          """[8,34,321443,{},"invalid uri"]""",
          """[8,9999,321443,{},"wamp.error.no_such_subscription"]""",
          """[8,34,471298473917493172,{},"wamp.error.no_such_subscription"]""",
          """[8,34,321443,{},"wamp.error.no_such_subscription",null]""",
          """[8,34,321443,{},"wamp.error.no_such_subscription",[],null]"""
        ).foreach { text =>
          a[SerializingException] mustBe thrownBy(s.deserialize(text))
        }
      }
      "succeed for valid ERROR without payload" in {
        s.deserialize("""[8, 34, 713845233, {}, "wamp.error.no_such_subscription"]""") match {
          case m: Error =>
            m.requestType mustBe 34
            m.requestId mustBe 713845233
            m.details mustBe empty
            m.error mustBe "wamp.error.no_such_subscription"
            m.payload mustBe None
          case _ => fail
        }
      }
      "succeed for valid ERROR with payload as list" in {
        s.deserialize(s"""[8, 34, 713845233, {}, "wamp.error.no_such_subscription", $PayloadListJson]""") match {
          case m: Error =>
            m.requestType mustBe 34
            m.requestId mustBe 713845233
            m.details mustBe empty
            m.error mustBe "wamp.error.no_such_subscription"
            m.payload.value.arguments.left.value mustBe PayloadListObj.arguments.left.get
          case _ => fail
        }
      }
      "succeed for valid ERROR with payload as map" in {
        s.deserialize(s"""[8, 34, 713845233, {}, "wamp.error.no_such_subscription", [], $PayloadMapJson]""") match {
          case m: Error =>
            m.requestType mustBe 34
            m.requestId mustBe 713845233
            m.details mustBe empty
            m.error mustBe "wamp.error.no_such_subscription"
            m.payload.value.arguments.right.value mustBe PayloadMapObj.arguments.right.get
            /*m.payload match {
              case Some(Payload(Left(args))) => args mustBe PayloadMapObj.arguments.right.get
              case _ => null
            }*/
          case _ => fail
        }
      }


      //[PUBLISH, Request|id, Options|dict, Topic|uri]
      //[PUBLISH, Request|id, Options|dict, Topic|uri, Arguments|list]
      //[PUBLISH, Request|id, Options|dict, Topic|uri, Arguments|list, ArgumentsKw|dict]
      "fail for invalid PUBLISH" in {
        List(
          """[16]""",
//          """[16,null]""",
//          """[16,321443,null]""",
//          """[16,321443,{},null]""",
//          """[16,321443,{},"invalid uri"]""",
//          """[16,471298473917493172,{},"myapp.topic1"]""",
//          """[16,471298473917493172,{},"invalid topic"]""",
//          """[16,321443,{},"myapp.topic1",null]""",
          """[16,321443,{},"myapp.topic1",[],null]"""
        ).foreach { text =>
          a[SerializingException] mustBe thrownBy(s.deserialize(text))
        }
      }
      "succeed for valid PUBLISH without payload" in {
        s.deserialize("""[16, 713845233, {"acknowledge":  true}, "myapp.topic1"]"""") match {
          case m: Publish =>
            m.requestId mustBe 713845233
            m.options mustBe Map("acknowledge"->true)
            m.topic mustBe "myapp.topic1"
            m.payload mustBe None
          case _ =>
            fail("type mismatch")
        }
      }
      "succeed for valid PUBLISH with payload as list" in { 
        pending
      }
      "succeed for valid PUBLISH with payload as map" in {
        pending
      }

      
      
      //[PUBLISHED, PUBLISH.Request|id, Publication|id]
      "fail for invalid PUBLISHED" in {
        List(
          """[17]""",
          """[17,null]""",
          """[17,1431253]""",
          """[17,1431253,null]""",
          """[17,421341248931241243312432434,34]"""
        ).foreach { text =>
          a[SerializingException] mustBe thrownBy(s.deserialize(text))
        }
      }
      "succeed for valid PUBLISHED" in {
        s.deserialize("""[17, 239714735, 4429313566]""") match {
          case m: Published =>
            m.requestId mustBe 239714735
            m.publicationId mustBe 4429313566L
          case _ => fail
        }
      }


      //[SUBSCRIBE, Request|id, Options|dict, Topic|uri]
      "fail for invalid SUBSCRIBE" in {
        List(
          """[32]""",
          """[32,null]""",
          """[32,321443,null]""",
          """[32,321443,{},null]""",
          """[32,321443,{},"invalid uri"]""",
          """[32,471298473917493172,{},"myapp.topic1"]""",
          """[32,471298473917493172,{},"invalid topic"]"""
        ).foreach { text =>
          a[SerializingException] mustBe thrownBy(s.deserialize(text))
        }
      }
      "succeed for valid SUBSCRIBE" in {
        s.deserialize("""[32, 713845233, {}, "topic1"]""") match {
          case m: Subscribe =>
            m.requestId mustBe 713845233
            m.options must have size(0)
            m.topic mustBe "topic1"
          case _ => fail
        }
      }


      //[SUBSCRIBED, SUBSCRIBE.Request|id, Subscription|id]
      "fail for invalid SUBSCRIBED" in {
        List(
          """[33]""",
          """[33,null]""",
          """[33,1431253]""",
          """[33,1431253,null]""",
          """[33,421341248931241243312432434,34]"""
        ).foreach { text =>
          a[SerializingException] mustBe thrownBy(s.deserialize(text))
        }
      }
      "succeed for valid SUBSCRIBED" in {
        s.deserialize("""[33, 713845233, 5512315355]""") match {
          case m: Subscribed =>
            m.requestId mustBe 713845233
            m.subscriptionId mustBe 5512315355l
          case _ => fail
        }
      }

      
      //[UNSUBSCRIBE, Request|id, SUBSCRIBED.Subscription|id]
      "fail for invalid UNSUBSCRIBE" in {
        List(
          """[34]""",
          """[34,null]""",
          """[34,1431253]""",
          """[34,1431253,null]""",
          """[34,421341248931241243312432434,34]"""
        ).foreach { text =>
          a[SerializingException] mustBe thrownBy(s.deserialize(text))
        }
      }
      "succeed for valid UNSUBSCRIBE" in {
        s.deserialize("""[34, 713845233, 246643274]""") match {
          case m: Unsubscribe =>
            m.requestId mustBe 713845233
            m.subscriptionId mustBe 246643274
          case _ => fail
        }
      }


      //[UNSUBSCRIBED, UNSUBSCRIBE.Request|id]
      "fail for invalid UNSUBSCRIBED" in {
        List(
          """[35]""",
          """[35,null]""",
          """[35,421341248931241243312432434]"""
        ).foreach { text =>
          a[SerializingException] mustBe thrownBy(s.deserialize(text))
        }
      }
      "succeed for valid UNSUBSCRIBED" in {
        s.deserialize("""[35, 85346237]""") match {
          case m: Unsubscribed => 
            m.requestId mustBe 85346237
          case _ =>  fail
        }
      }

      
      //[EVENT, SUBSCRIBED.Subscription|id, PUBLISHED.Publication|id, Details|dict]
      //[EVENT, SUBSCRIBED.Subscription|id, PUBLISHED.Publication|id, Details|dict, Arguments|list]
      //[EVENT, SUBSCRIBED.Subscription|id, PUBLISHED.Publication|id, Details|dict, Arguments|list, ArgumentsKw|dict]
      "fail for invalid EVENT" in {
        pending
      }
      "succeed for valid EVENT without payload" in {
        pending
      }
      "succeed for valid EVENT with payload as list" in {
        pending
      }
      "succeed for valid EVENT with payload as map" in {
        pending
      }
    }
    
    
    "serializing to JSON" should {

      "serialize HELLO" in {
        val msg = Hello("akka.wamp.realm", Dict().withRoles("publisher"))
        val json = s.serialize(msg)
        json mustBe """[1,"akka.wamp.realm",{"roles":{"publisher":{}}}]"""
      }
      
      "serialize WELCOME" in {
        val msg = Welcome(1233242, Dict().withAgent("akka-wamp-0.2.0").withRoles("broker"))
        val json = s.serialize(msg)
        json mustBe """[2,1233242,{"agent":"akka-wamp-0.2.0","roles":{"broker":{}}}]"""
      }

      "serialize GOODBYE" in {
        val msg = Goodbye(Dict(), "wamp.error.goobye_and_out")
        val json = s.serialize(msg)
        json mustBe """[6,{},"wamp.error.goobye_and_out"]"""
      }

      "serialize ABORT" in {
        pending
      }

      "serialize ERROR" in {
        val msg1 = Error(SUBSCRIBE, 341284, Dict(), "wamp.error.no_such_subscription")
        val json1 = s.serialize(msg1)
        json1 mustBe """[8,32,341284,{},"wamp.error.no_such_subscription"]"""

        val msg2 = Error(SUBSCRIBE, 341284, Dict(), "wamp.error.no_such_subscription", Some(PayloadListObj))
        val json2 = s.serialize(msg2)
        json2 mustBe s"""[8,32,341284,{},"wamp.error.no_such_subscription",$PayloadListJson]"""

        val msg3 = Error(SUBSCRIBE, 341284, Dict(), "wamp.error.no_such_subscription", Some(PayloadMapObj))
        val json3 = s.serialize(msg3)
        json3 mustBe s"""[8,32,341284,{},"wamp.error.no_such_subscription",[],$PayloadMapJson]"""
      }

      "serialize PUBLISHED" in {
        val msg = Published(713845233, 5512315)
        val json = s.serialize(msg)
        json mustBe """[17,713845233,5512315]"""
      }
      
      "serialize SUBSCRIBE" in {
        val msg = Subscribe(1, Dict(), "myapp.topic1")
        val json = s.serialize(msg)
        json mustBe """[32,1,{},"myapp.topic1"]"""
      }
      
      "serialize SUBSCRIBED" in {
        val msg = Subscribed(713845233, 5512315)
        val json = s.serialize(msg)
        json mustBe """[33,713845233,5512315]"""
      }

      "serialize UNSUBSCRIBED" in {
        val msg = Unsubscribed(85346237)
        val json = s.serialize(msg)
        json mustBe """[35,85346237]"""
      }

      "serialize EVENT" in {
        pending
        val msg1 = Event(713845233, 5512315, Dict())
        val json1 = s.serialize(msg1)
        json1 mustBe """[36,713845233,5512315,{}]"""
        
        val msg2 = Event(713845233, 5512315, Dict(), Some(PayloadListObj))
        val json2 = s.serialize(msg2)
        json2 mustBe s"""[36,713845233,5512315,{},$PayloadListJson]"""

        val msg3 = Event(713845233, 5512315, Dict(), Some(PayloadMapObj))
        val json3 = s.serialize(msg3)
        json3 mustBe s"""[36,713845233,5512315,{},[],$PayloadMapJson]"""
      }

    }
  }
  
  val PayloadListJson = """["paolo",42.5,true,null,["nested"],{"key":"value"}]"""
  val PayloadListObj = Payload(List("paolo", 42.5, true, null, List("nested"), Map("key" -> "value")))
  val PayloadMapJson = """{"name":"paolo","age":42.5,"object":{"nested":"value"},"array":["value"],"male":true,"notes":null}"""
  val PayloadMapObj = Payload(Map("name"->"paolo", "age"->42.5, "male"->true, "notes"->null, "array"->List("value"), "object"->Map("nested"->"value")))
}
