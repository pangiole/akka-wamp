package akka.wamp.serialization

import akka.wamp._
import akka.wamp.messages._

class JsonDeserializerSpec extends SerializingBaseSpec {

  val s = new JsonSerialization

  "The wamp.2.json deserializer" should "fail on invalid text messages" in {
    List(
      """  { noscan """,
      """["invalid",noscan] """,
      """[null,""",
      """[999,noscan] """
    ).foreach { text =>
      a[DeserializeException] mustBe thrownBy(s.deserialize(source(text)))
    }
  }

  //[HELLO, Realm|uri, Details|dict]
  it should "fail on invalid HELLO text messages" in {
    List(
      """[1]""",
      """[1,null]""",
      """[1,"myapp.realm"]""",
      """[1,"myapp.realm",null]""",
      """[1,"myapp.realm",{}]"""",
      """[1,"myapp.realm",{"INVALID":null}]"""",
      """[1,"myapp.realm",{"roles":null}]"""",
      """[1,"myapp.realm",{"roles":{}}]"""",
      """[1,"myapp.realm",{"roles":{"invalid":{}}}]"""",
      """[1,"invalid..realm",{"roles":{"publisher":{}}}]"""",
      """[1,"invalid..realm",{"roles":{"publisher":{"should.not.be","here"}}}]""""
    ).foreach { text =>
      a[DeserializeException] mustBe thrownBy(s.deserialize(source(text)))
    }
  }

  it should "deserialize a valid HELLO text message" in {
    s.deserialize(source("""[  1  ,"myapp.realm",  {"roles":{"caller":{},"callee":{}}}]""")) match {
      case msg: Hello =>
        msg.realm mustBe "myapp.realm"
        msg.details mustBe Dict("roles" -> Dict("caller" -> Dict(), "callee" -> Dict()))
      case msg =>
        fail(s"Unexpected $msg")
    }
  }

  //[WELCOME, Session|id, Details|dict]
  it should "fail on invalid WELCOME text messages" in {
    List(
      """[2]""",
      """[2,null]""",
      """[2,1]""",
      """[2,1,null]""",
      """[2,1,{"123":null}]""",
      """[2,0,{"roles":{"broker":{}}}]""",
      """[2,9007199254740993,{"roles":{"broker":{}}}]"""
    ).foreach { text =>
      a[DeserializeException] mustBe thrownBy(s.deserialize(source(text)))
    }
  }

  it should "deserialize a valid WELCOME text message" in {
    s.deserialize(source("""[2,9007199254740992,{"roles":{"broker":{}}}]""")) match {
      case msg: Welcome =>
        msg.sessionId mustBe 9007199254740992L
        msg.details mustBe Dict("roles" -> Dict("broker" -> Dict()))
      case msg =>
        fail(s"Unexpected $msg")
    }
  }


  //[ABORT, Details|dict, Reason|uri]
  it should "fail on invalid ABORT text messages" in {
    List(
      """[3]""",
      """[3,null]""",
      """[3,{}]""",
      """[3,{},null]""",
      """[3,{},{"ab":null}]"""
    ).foreach { text =>
      a[DeserializeException] mustBe thrownBy(s.deserialize(source(text)))
    }
  }

  it should "deserialize a valid ABORT text message" in {
    s.deserialize(source("""[3, {"message": "The realm does not exist."},"wamp.error.no_such_realm"]""")) match {
      case message: Abort =>
        message.details mustBe Dict("message" -> "The realm does not exist.")
        message.reason mustBe "wamp.error.no_such_realm"
      case _ => fail
    }
  }


  //[GOODBYE, Details|dict, Reason|uri]
  it should "fail on invalid GOODBYE text messages" in {
    List(
      """[6]""",
      """[6,null]""",
      """[6,{}]""",
      """[6,{},null]""",
      """[6,{},"invalid..reason"]"""
    ).foreach { text =>
      a[DeserializeException] mustBe thrownBy(s.deserialize(source(text)))
    }
  }

  it should "deserialize a valid GOODBYE text message" in {
    s.deserialize(source("""[6,{"message": "The host is shutting down now."},"akka.wamp.system_shutdown"]""")) match {
      case msg: Goodbye =>
        msg.details mustBe Dict("message" -> "The host is shutting down now.")
        msg.reason mustBe "akka.wamp.system_shutdown"
      case msg =>
        fail(s"Unexpected $msg")
    }
  }


  //[ERROR, REQUEST.Type|int, REQUEST.Request|id, Details|dict, Error|uri]
  //[ERROR, REQUEST.Type|int, REQUEST.Request|id, Details|dict, Error|uri, Arguments|list]
  //[ERROR, REQUEST.Type|int, REQUEST.Request|id, Details|dict, Error|uri, Arguments|list, ArgumentsKw|dict]
  it should "fail on invalid ERROR text messages" in {
    List(
      """[8]""",
      """[8,null]""",
      """[8,34,null]""",
      """[8,34,1,null]""",
      """[8,34,1,{},null]""",
      """[8,34,1,{},"invalid..error"]""",
      """[8,34,0,{},"wamp.error.no_such_subscription"]""",
      """[8,34,9007199254740993,{},"wamp.error.no_such_subscription"]"""
    ).foreach { text =>
      a[DeserializeException] mustBe thrownBy(s.deserialize(source(text)))
    }
  }

  it should "deserialize a valid ERROR text message" in {
    s.deserialize(source("""[8,34,9007199254740992,{},"wamp.error.no_such_subscription"]""")) match {
      case msg: Error =>
        msg.requestType mustBe 34
        msg.requestId mustBe 9007199254740992L
        msg.details mustBe empty
        msg.error mustBe "wamp.error.no_such_subscription"
      case msg =>
        fail(s"Unexpected $msg")
    }
  }


  //[PUBLISH, Request|id, Options|dict, Topic|uri]
  //[PUBLISH, Request|id, Options|dict, Topic|uri, Arguments|list]
  //[PUBLISH, Request|id, Options|dict, Topic|uri, Arguments|list, ArgumentsKw|dict]
  it should "fail on invalid PUBLISH text messages" in {
    List(
      """[16]""",
      """[16,null]""",
      """[16,1,null]""",
      """[16,1,{},null]""",
      """[16,1,{},"invalid..topic"]""",
      """[16,0,{},"myapp.topic"]""",
      """[16,9007199254740993,{},"myapp.topic"]"""
    ).foreach { text =>
      a[DeserializeException] mustBe thrownBy(s.deserialize(source(text)))
    }
  }

  it should "deserialize a valid PUBLISH text message" in {
    s.deserialize(source("""[16,9007199254740992,{"acknowledge":true},"myapp.topic"]"""")) match {
      case msg: Publish =>
        msg.requestId mustBe 9007199254740992L
        msg.options mustBe Dict("acknowledge" -> true)
        msg.topic mustBe "myapp.topic"
      case msg =>
        fail(s"Unexpected $msg")
    }
  }


  //[PUBLISHED, PUBLISH.Request|id, Publication|id]
  it should "fail on invalid PUBLISHED text messages" in {
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
      a[DeserializeException] mustBe thrownBy(s.deserialize(source(text)))
    }
  }

  it should "deserialize a valid PUBLISHED text message" in {
    s.deserialize(source("""[17,9007199254740988,9007199254740992]""")) match {
      case p: Published =>
        p.requestId mustBe 9007199254740988L
        p.publicationId mustBe 9007199254740992L
      case _ => fail
    }
  }


  //[SUBSCRIBE, Request|id, Options|dict, Topic|uri]
  it should "fail on invalid SUBSCRIBE text messages" in {
    List(
      """[32]""",
      """[32,null]""",
      """[32,1,null]""",
      """[32,1,{},null]""",
      """[32,1,{},"invalid..uri"]""",
      """[32,0,{},"myapp.topic"]""",
      """[32,9007199254740993,{},"myapp.topic"]"""
    ).foreach { text =>
      a[DeserializeException] mustBe thrownBy(s.deserialize(source(text)))
    }
  }

  it should "deserialize valid SUBSCRIBE text message" in {
    s.deserialize(source("""[32,9007199254740992,{},"myapp.topic"]""")) match {
      case msg: Subscribe =>
        msg.requestId mustBe 9007199254740992L
        msg.options mustBe empty
        msg.topic mustBe "myapp.topic"
      case msg =>
        fail(s"Unexpected $msg")
    }
  }


  //[SUBSCRIBED, SUBSCRIBE.Request|id, Subscription|id]
  it should "fail on invalid SUBSCRIBED text messages" in {
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
      a[DeserializeException] mustBe thrownBy(s.deserialize(source(text)))
    }
  }

  it should "deserialize a valid SUBSCRIBED text message" in {
    s.deserialize(source("""[33,9007199254740977,9007199254740992]""")) match {
      case msg: Subscribed =>
        msg.requestId mustBe 9007199254740977L
        msg.subscriptionId mustBe 9007199254740992L
      case msg =>
        fail(s"Unexpected $msg")
    }
  }


  //[UNSUBSCRIBE, Request|id, SUBSCRIBED.Subscription|id]
  it should "fail on invalid UNSUBSCRIBE text messages" in {
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
      a[DeserializeException] mustBe thrownBy(s.deserialize(source(text)))
    }
  }

  it should "deserialize a valid UNSUBSCRIBE text message" in {
    s.deserialize(source("""[34,9007199254740955,9007199254740992]""")) match {
      case msg: Unsubscribe =>
        msg.requestId mustBe 9007199254740955L
        msg.subscriptionId mustBe 9007199254740992L
      case msg =>
        fail(s"Unexpected $msg")
    }
  }


  //[UNSUBSCRIBED, UNSUBSCRIBE.Request|id]
  it should "fail on invalid UNSUBSCRIBED text messages" in {
    List(
      """[35]""",
      """[35,null]""",
      """[35,0]""",
      """[35,9007199254740993]"""
    ).foreach { text =>
      a[DeserializeException] mustBe thrownBy(s.deserialize(source(text)))
    }
  }

  it should "deserialize a valid UNSUBSCRIBED text message" in {
    s.deserialize(source("""[35,9007199254740992]""")) match {
      case msg: Unsubscribed =>
        msg.requestId mustBe 9007199254740992L
      case msg =>
        fail(s"Unexpected $msg")
    }
  }


  //[EVENT, SUBSCRIBED.Subscription|id, PUBLISHED.Publication|id, Details|dict]
  //[EVENT, SUBSCRIBED.Subscription|id, PUBLISHED.Publication|id, Details|dict, Arguments|list]
  //[EVENT, SUBSCRIBED.Subscription|id, PUBLISHED.Publication|id, Details|dict, Arguments|list, ArgumentsKw|dict]
  it should "fail on invalid EVENT text messages" in {
    List(
      """[36]""",
      """[36,null]""",
      """[36,1,null]""",
      """[36,1,2,null]""",
      """[36,0,2,{}]""",
      """[36,9007199254740993,2,{}]""",
      """[36,1,0,{}]""",
      """[36,1,9007199254740993,{}]"""
    ).foreach { text =>
      a[DeserializeException] mustBe thrownBy(s.deserialize(source(text)))
    }
  }

  it should "deserialize a valid EVENT text message" in {
    s.deserialize(source("""[36,9007199254740933,9007199254740992,{}]""")) match {
      case msg: Event =>
        msg.subscriptionId mustBe 9007199254740933L
        msg.publicationId mustBe 9007199254740992L
        msg.details mustBe empty
      case msg =>
        fail(s"Unexpected $msg")
    }
  }

  //[REGISTER, Request|id, Options|dict, Topic|uri]
  it should "fail on invalid REGISTER text messages" in {
    List(
      """[64]""",
      """[64,null]""",
      """[64,1,null]""",
      """[64,1,{},null]""",
      """[64,1,{},"invalid..uri"]""",
      """[64,0,{},"myapp.procedure1"]""",
      """[64,9007199254740993,{},"myapp.procedure1"]"""
    ).foreach { text =>
      a[DeserializeException] mustBe thrownBy(s.deserialize(source(text)))
    }
  }


  it should "deserialize a valid REGISTER text message" in {
    s.deserialize(source("""[64,9007199254740992,{},"myapp.procedure1"]""")) match {
      case msg: Register =>
        msg.requestId mustBe 9007199254740992L
        msg.options mustBe empty
        msg.procedure mustBe "myapp.procedure1"
      case msg =>
        fail(s"Unexpected $msg")
    }
  }


  //[REGISTERED, REGISTER.Request|id, Subscription|id]
  it should "fail on invalid REGISTERED text messages" in {
    List(
      """[65]""",
      """[65,null]""",
      """[65,1]""",
      """[65,1,null]""",
      """[65,0,2]""",
      """[65,9007199254740993,2]""",
      """[65,1,0]""",
      """[65,1,9007199254740993]"""
    ).foreach { text =>
      a[DeserializeException] mustBe thrownBy(s.deserialize(source(text)))
    }
  }

  it should "deserialize a valid REGISTERED text message" in {
    s.deserialize(source("""[65,9007199254740977,9007199254740992]""")) match {
      case msg: Registered =>
        msg.requestId mustBe 9007199254740977L
        msg.registrationId mustBe 9007199254740992L
      case msg =>
        fail(s"Unexpected $msg")
    }
  }


  //[UNREGISTER, Request|id, REGISTERED.Registration|id]
  it should "fail on invalid UNREGISTER text messages" in {
    List(
      """[66]""",
      """[66,null]""",
      """[66,1]""",
      """[66,1,null]""",
      """[66,0,2]""",
      """[66,9007199254740993,2]""",
      """[66,1,0]""",
      """[66,1,9007199254740993]"""
    ).foreach { text =>
      a[DeserializeException] mustBe thrownBy(s.deserialize(source(text)))
    }
  }

  it should "deserialize a valid UNREGISTER text message" in {
    s.deserialize(source("""[66,9007199254740955,9007199254740992]""")) match {
      case msg: Unregister =>
        msg.requestId mustBe 9007199254740955L
        msg.registrationId mustBe 9007199254740992L
      case msg =>
        fail(s"Unexpected $msg")
    }
  }


  //[UNREGISTERED, UNREGISTER.Request|id]
  it should "fail on invalid UNREGISTERED text messages" in {
    List(
      """[67]""",
      """[67,null]""",
      """[67,0]""",
      """[67,9007199254740993]"""
    ).foreach { text =>
      a[DeserializeException] mustBe thrownBy(s.deserialize(source(text)))
    }
  }

  it should "deserialize a valid UNREGISTERD text message" in {
    s.deserialize(source("""[67,9007199254740992]""")) match {
      case msg: Unregistered =>
        msg.requestId mustBe 9007199254740992L
      case msg =>
        fail(s"Unexpected $msg")
    }
  }


  //[CALL, Request|id, Options|dict, Procedure|uri]
  //[CALL, Request|id, Options|dict, Procedure|uri, Arguments|list]
  //[CALL, Request|id, Options|dict, Procedure|uri, Arguments|list, ArgumentsKw|dict]
  it should "fail on invalid CALL text messages" in {
    List(
      """[48]""",
      """[48,null]""",
      """[48,1,null]""",
      """[48,1,{},null]""",
      """[48,1,{},"invalid..procedure"]""",
      """[48,0,{},"myapp.procedure"]""",
      """[48,9007199254740993,{},"myapp.procedure"]"""
    ).foreach { text =>
      a[DeserializeException] mustBe thrownBy(s.deserialize(source(text)))
    }
  }

  it should "deserialize a valid CALL text message" in {
    s.deserialize(source("""[48,9007199254740992,{},"myapp.procedure"]"""")) match {
      case msg: Call =>
        msg.requestId mustBe 9007199254740992L
        msg.options mustBe empty
        msg.procedure mustBe "myapp.procedure"
      case msg =>
        fail(s"unexpected $msg")
    }
  }

  //[INVOCATION, Request|id, REGISTERED.Registration|id, Details|dict]
  //[INVOCATION, Request|id, REGISTERED.Registration|id, Details|dict, CALL.Arguments|list]
  //[INVOCATION, Request|id, REGISTERED.Registration|id, Details|dict, CALL.Arguments|list, ArgumentsKw|dict]
  it should "fail on invalid INVOCATION text messages" in {
    List(
      """[68]""",
      """[68,null]""",
      """[68,1,null]""",
      """[68,1,1,null]""",
      """[68,0,1,{}]""",
      """[68,9007199254740993,1,{}]""",
      """[68,1,0,{}]""",
      """[68,1,9007199254740993,{}]"""
    ).foreach { text =>
      a[DeserializeException] mustBe thrownBy(s.deserialize(source(text)))
    }
  }

  it should "deserialize a valid INVOCATION text message" in {
    s.deserialize(source("""[68,1,1,{}]"""")) match {
      case msg: Invocation =>
        msg.requestId mustBe 1
        msg.registrationId mustBe 1
        msg.details mustBe Dict()
      case msg =>
        fail(s"unexpected $msg")
    }
  }


  //[YIELD, INVOCATION.Request|id, Options|dict]
  //[YIELD, INVOCATION.Request|id, Options|dict, Arguments|list]
  //[YIELD, INVOCATION.Request|id, Options|dict, Arguments|list, ArgumentsKw|dict]
  it should "fail on invalid YIELD text messages" in {
    List(
      """[70]""",
      """[70,null]""",
      """[70,1,null]""",
      """[70,0,{}]""",
      """[70,9007199254740993,{}]"""
    ).foreach { text =>
      a[DeserializeException] mustBe thrownBy(s.deserialize(source(text)))
    }
  }

  it should "deserialize a valid YIELD text message" in {
    s.deserialize(source("""[70,1,{}]"""")) match {
      case msg: Yield =>
        msg.requestId mustBe 1
        msg.options mustBe Dict()
      case msg =>
        fail(s"unexpected $msg")
    }
  }

  //[RESULT, CALL.Request|id, Details|dict]
  //[RESULT, CALL.Request|id, Details|dict, YIELD.Arguments|list]
  //[RESULT, CALL.Request|id, Details|dict, YIELD.Arguments|list, ArgumentsKw|dict]
  it should "fail on invalid RESULT text messages" in {
    List(
      """[50]""",
      """[50,null]""",
      """[50,1,null]""",
      """[50,0,{}]""",
      """[50,9007199254740993,{}]"""
    ).foreach { text =>
      a[DeserializeException] mustBe thrownBy(s.deserialize(source(text)))
    }
  }

  it should "deserialize a valid RESULT text message" in {
    s.deserialize(source("""[50,1,{}]"""")) match {
      case msg: Result =>
        msg.requestId mustBe 1
        msg.details mustBe Dict()
      case msg =>
        fail(s"unexpected $msg")
    }
  }
}

