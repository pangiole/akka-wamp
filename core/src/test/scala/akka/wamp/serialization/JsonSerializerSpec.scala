package akka.wamp.serialization

import akka.stream.scaladsl.Source
import akka.wamp._
import akka.wamp.messages._
import com.fasterxml.jackson.core.JsonFactory


class JsonSerializerSpec extends SerializingBaseSpec 
{
  val s = new JsonSerialization(new JsonFactory())

  "The json serializer" should  "serialize Hello object messages" in {
    val message = Hello("default", Dict().withRoles(Roles.client))
    whenReduced(s.serialize(message)) { json =>
      json mustBe """[1,"default",{"roles":{"callee":{},"caller":{},"publisher":{},"subscriber":{}}}]"""
    }
  }

  
  it should "serialize Welcome object messages" in {
    val message = Welcome(1233242, Dict().withAgent("akka-wamp-0.15.0").withRoles("broker"))
    whenReduced(s.serialize(message)) { json =>
      json mustBe """[2,1233242,{"agent":"akka-wamp-0.15.0","roles":{"broker":{}}}]"""
    }
  }

  
  it should "serialize Goodbye object messages" in {
    val message = Goodbye(Dict(), "wamp.error.goobye_and_out")
    whenReduced(s.serialize(message)) { json =>
      json mustBe """[6,{},"wamp.error.goobye_and_out"]"""
    }
  }

  
  it should "serialize Abort object messages" in {
    val message = Abort(Dict("message" -> s"The realm 'unknown' does not exist."), "wamp.error.no_such_realm")
    whenReduced(s.serialize(message)) { json =>
      json mustBe """[3,{"message":"The realm 'unknown' does not exist."},"wamp.error.no_such_realm"]"""
    }
  }

  
  it should "serialize Error object messages" in {
    val message1 = Error(Subscribe.tpe, 12345, Error.defaultDict, "wamp.error.no_such_subscription")
    whenReduced(s.serialize(message1)) { json =>
      json mustBe """[8,32,12345,{},"wamp.error.no_such_subscription"]"""
    }
    val message2 = Error(Publish.tpe, 67890, Error.defaultDict, "wamp.error.no_such_topic", Payload(args))
    whenReduced(s.serialize(message2)) { json =>
      json mustBe s"""[8,16,67890,{},"wamp.error.no_such_topic",$argsJson]"""
    }
    val message3 = Error(Call.tpe, 67890, Error.defaultDict, "wamp.error.no_such_procedure", Payload(kwargs))
    whenReduced(s.serialize(message3)) { json =>
      json mustBe s"""[8,48,67890,{},"wamp.error.no_such_procedure",[],$kwargsJson]"""
    }
    val message4 = Error(Invocation.tpe, 45678, Error.defaultDict, "wamp.error.no_such_procedure", Payload(args, kwargs))
    whenReduced(s.serialize(message4)) { json =>
      json mustBe s"""[8,68,45678,{},"wamp.error.no_such_procedure",$argsJson,$kwargsJson]"""
    }
  }


  it should "serialize Publish object messages" in {
    val message1 = Publish(12345, Publish.defaultOptions, "myapp.topic1")
    whenReduced(s.serialize(message1)) { json =>
      json mustBe """[16,12345,{},"myapp.topic1"]"""
    }
    val message2 = Publish(67890, Publish.defaultOptions, "myapp.topic2", Payload(args))
    whenReduced(s.serialize(message2)) { json =>
      json mustBe s"""[16,67890,{},"myapp.topic2",$argsJson]"""
    }
    val message3 = Publish(45678, Publish.defaultOptions, "myapp.topic3", Payload(kwargs))
    whenReduced(s.serialize(message3)) { json =>
      json mustBe s"""[16,45678,{},"myapp.topic3",[],$kwargsJson]"""
    }
    val message4 = Publish(102938, Publish.defaultOptions, "myapp.topic4", Payload(args, kwargs))
    whenReduced(s.serialize(message4)) { json =>
      json mustBe s"""[16,102938,{},"myapp.topic4",$argsJson,$kwargsJson]"""
    }
  }

  
  it should "serialize Published object messages" in {
    val message = Published(713845233, 5512315)
    whenReduced(s.serialize(message)) { json =>
      json mustBe """[17,713845233,5512315]"""
    }
  }

  
  it should "serialize Subscribe object messages" in {
    val message = Subscribe(1, options = Dict(), "myapp.topic")
    whenReduced(s.serialize(message)) { json =>
      json mustBe """[32,1,{},"myapp.topic"]"""
    }
  }

  
  it should "serialize Subscribed object messages" in {
    val message = Subscribed(713845233, 5512315)
    whenReduced(s.serialize(message)) { json =>
      json mustBe """[33,713845233,5512315]"""
    }
  }

  
  it should "serialize Unsubscribe object messages" in {
    val message = Unsubscribe(85346237, 984348843)
    whenReduced(s.serialize(message)) { json =>
      json mustBe """[34,85346237,984348843]"""
    }
  }

  
  it should "serialize Unsubscribed object messages" in {
    val message = Unsubscribed(85346237)
    whenReduced(s.serialize(message)) { json =>
      json mustBe """[35,85346237]"""
    }
  }

  
  it should "serialize Event object messages" in {
    val message1 = Event(12345, 67890)
    whenReduced(s.serialize(message1)) { json =>
      json mustBe s"""[36,12345,67890,{}]"""
    }
    val message2 = Event(54321, 98765, Event.defaultOptions, Payload(args))
    whenReduced(s.serialize(message2)) { json =>
      json mustBe s"""[36,54321,98765,{},$argsJson]"""
    }
    val message3 = Event(102938, 9012, Event.defaultOptions, Payload(kwargs))
    whenReduced(s.serialize(message3)) { json =>
      json mustBe s"""[36,102938,9012,{},[],$kwargsJson]"""
    }
    val message4 = Event(713845233, 5512315, Event.defaultOptions, Payload(args, kwargs))
    whenReduced(s.serialize(message4)) { json =>
      json mustBe s"""[36,713845233,5512315,{},$argsJson,$kwargsJson]"""
    }
  }

  
  it should "serialize Register object messages" in {
    val message = Register(1, Dict(), "myapp.procedure")
    whenReduced(s.serialize(message)) { json =>
      json mustBe """[64,1,{},"myapp.procedure"]"""
    }
  }

  
  it should "serialize Registered object messages" in {
    val message = Registered(713845233, 5512315)
    whenReduced(s.serialize(message)) { json =>
      json mustBe """[65,713845233,5512315]"""
    }
  }

  
  it should "serialize Unregister object messages" in {
    val message = Unregister(85346237, 43784343)
    whenReduced(s.serialize(message)) { json =>
      json mustBe """[66,85346237,43784343]"""
    }
  }

  
  it should "serialize Unregistered object messages" in {
    val message = Unregistered(85346237)
    whenReduced(s.serialize(message)) { json =>
      json mustBe """[67,85346237]"""
    }
  }

  
  it should "serialize Call object messages" in {
    val message1 = Call(12345, Call.defaultOptions, "myapp.procedure1")
    whenReduced(s.serialize(message1)) { json =>
      json mustBe """[48,12345,{},"myapp.procedure1"]"""
    }
    val message2 = Call(67890, Call.defaultOptions, "myapp.procedure2", Payload(args))
    whenReduced(s.serialize(message2)) { json =>
      json mustBe s"""[48,67890,{},"myapp.procedure2",$argsJson]"""
    }
    val message3 = Call(54321, Call.defaultOptions, "myapp.procedure3", Payload(kwargs))
    whenReduced(s.serialize(message3)) { json =>
      json mustBe s"""[48,54321,{},"myapp.procedure3",[],$kwargsJson]"""
    }
    val message4 = Call(98765, Call.defaultOptions, "myapp.procedure4", Payload(args, kwargs))
    whenReduced(s.serialize(message4)) { json =>
      json mustBe s"""[48,98765,{},"myapp.procedure4",$argsJson,$kwargsJson]"""
    }
  }

  
  it should "serialize Invocation object messages" in {
    val message1 = Invocation(9, 12345, Invocation.defaultDetails)
    whenReduced(s.serialize(message1)) { json =>
      json mustBe """[68,9,12345,{}]"""
    }
    val message2 = Invocation(9, 67890, Invocation.defaultDetails, Payload(args))
    whenReduced(s.serialize(message2)) { json =>
      json mustBe s"""[68,9,67890,{},$argsJson]"""
    }
    val message3 = Invocation(9, 4567, Invocation.defaultDetails, Payload(kwargs))
    whenReduced(s.serialize(message3)) { json =>
      json mustBe s"""[68,9,4567,{},[],$kwargsJson]"""
    }
    val message4 = Invocation(9, 431, Invocation.defaultDetails, Payload(args, kwargs))
    whenReduced(s.serialize(message4)) { json =>
      json mustBe s"""[68,9,431,{},$argsJson,$kwargsJson]"""
    }
  }

  
  it should "serialize Yield object messages" in {
    val message1 = Yield(9, Yield.defaultOptions)
    whenReduced(s.serialize(message1)) { json =>
      json mustBe """[70,9,{}]"""
    }
    val message2 = Yield(9, Yield.defaultOptions, Payload(args))
    whenReduced(s.serialize(message2)) { json =>
      json mustBe s"""[70,9,{},$argsJson]"""
    }
    val message3 = Yield(9, Yield.defaultOptions, Payload(kwargs))
    whenReduced(s.serialize(message3)) { json =>
      json mustBe s"""[70,9,{},[],$kwargsJson]"""
    }
    val message4 = Yield(9, Yield.defaultOptions, Payload(args, kwargs))
    whenReduced(s.serialize(message4)) { json =>
      json mustBe s"""[70,9,{},$argsJson,$kwargsJson]"""
    }
  }

  
  it should "serialize Result object messages" in {
    val message1 = Result(9, Result.defaultDetails)
    whenReduced(s.serialize(message1)) { json =>
      json mustBe """[50,9,{}]"""
    }

    val message2 = Result(9, Result.defaultDetails, Payload(args))
    whenReduced(s.serialize(message2)) { json =>
      json mustBe s"""[50,9,{},$argsJson]"""
    }

    val message3 = Result(9, Result.defaultDetails, Payload(kwargs))
    whenReduced(s.serialize(message3)) { json =>
      json mustBe s"""[50,9,{},[],$kwargsJson]"""
    }

    val message4 = Result(9, Result.defaultDetails, Payload(args, kwargs))
    whenReduced(s.serialize(message4)) { json =>
      json mustBe s"""[50,9,{},$argsJson,$kwargsJson]"""
    }
  }
  
  def whenReduced(source: Source[String, _])(testCode: (String) => Unit): Unit = {
    whenReady(source.runReduce(_ + _)) { text =>
      testCode(text)
    }
  }

  
  val args = List("paolo", 40, 1234567890, 34.56, List("array"), Map("key"->"value"), true, false, None)
  
  val argsJson = """["paolo",40,1234567890,34.56,["array"],{"key":"value"},true,false,null]"""
  
  val kwargs = Dict("string"->"paolo","int"->40,"long"->1234567890,"double"->34.56,"array"->List("array"),"object"->Map("key"->"value"),"true"->true,"false"->false,"null"->None)

  //val kwargsJson = """{"string":"paolo","int":40,"long":1234567890,"double":34.56,"array":["array"],"object":{"key":"value"},"true":true,"false":false,"null":null}"""
  val kwargsJson = """{"false":false,"string":"paolo","null":null,"object":{"key":"value"},"double":34.56,"array":["array"],"long":1234567890,"int":40,"true":true}"""
}

