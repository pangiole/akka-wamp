package akka.wamp.serialization


import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.wamp.messages._
import akka.wamp._

import org.scalatest._
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent._
import scala.concurrent.duration._


class JsonSerializerSpec extends FlatSpec
  with MustMatchers
  with TryValues 
  with OptionValues
  with EitherValues 
  with ScalaFutures
  with ParallelTestExecution
  with BeforeAndAfterAll 
{

  implicit val system = ActorSystem("test")
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  override protected def afterAll(): Unit = {
    materializer.shutdown()
    system.terminate()
    Await.ready(system.whenTerminated, 10.seconds)
  }

  implicit val validator = new Validator(strictUris = false)

  val s = new JsonSerialization

  "The wamp.2.json serializer" should  "serialize Hello object message" in {
    val message = Hello("akka.wamp.realm", Dict().addRoles(Roles.client))
    whenReduced(s.serialize(message)) { json =>
      json mustBe """[1,"akka.wamp.realm",{"roles":{"callee":{},"caller":{},"publisher":{},"subscriber":{}}}]"""
    }
  }

  it should "serialize Welcome object message" in {
    val message = Welcome(1233242, Dict().setAgent("akka-wamp-0.8.0").addRoles(Roles.broker))
    whenReduced(s.serialize(message)) { json =>
      json mustBe """[2,1233242,{"agent":"akka-wamp-0.8.0","roles":{"broker":{}}}]"""
    }
  }

  it should "serialize Goodbye object message" in {
    val message = Goodbye(Dict(), "wamp.error.goobye_and_out")
    whenReduced(s.serialize(message)) { json =>
      json mustBe """[6,{},"wamp.error.goobye_and_out"]"""
    }
  }

  it should "serialize Abort object message" in {
    val message = Abort(Dict("message" -> s"The realm 'unknown' does not exist."), "wamp.error.no_such_realm")
    whenReduced(s.serialize(message)) { json =>
      json mustBe """[3,{"message":"The realm 'unknown' does not exist."},"wamp.error.no_such_realm"]"""
    }
  }

  // TODO try with some recursive serialization
  // TODO stay DRY with serializing Payloads
  
  it should "serialize Error object message" in {
    pending
    val message1 = Error(Subscribe.tpe, 341284, Error.defaultDetails, "wamp.error.no_such_subscription")
    whenReduced(s.serialize(message1)) { json =>
      json mustBe """[8,32,341284,{},"wamp.error.no_such_subscription"]"""
    }

    val message2 = Error(Subscribe.tpe, 341284, Error.defaultDetails, "wamp.error.no_such_subscription", Payload(List(null, "paolo", 40, true)))
    whenReduced(s.serialize(message2)) { json =>
      json mustBe s"""[8,32,341284,{},"wamp.error.no_such_subscription",[null,"paolo",40,true]]"""
    }

    val message3 = Error(Subscribe.tpe, 341284, Error.defaultDetails, "wamp.error.no_such_subscription", Payload(Dict("arg0"->"paolo", "age"->40, "arg2"->true)))
    whenReduced(s.serialize(message3)) { json =>
      json mustBe """[8,32,341284,{},"wamp.error.no_such_subscription",[],{"arg0":"paolo","age":40,"arg2":true}]"""
    }
  }


  it should "serialize Publish object message" in {
    pending
    val message1 = Publish(341284, options = Dict(), "myapp.topic")
    whenReduced(s.serialize(message1)) { json =>
      json mustBe """[16,341284,{},"myapp.topic"]"""
    }

    val message2 = Publish(341284, options = Dict(), "myapp.topic", Payload(List(None, "paolo", 40, true)))
    whenReduced(s.serialize(message2)) { json =>
      json mustBe """[16,341284,{},"myapp.topic",[null,"paolo",40,true]]"""
    }

    val message3 = Publish(341284, options = Dict(), "myapp.topic", Payload(List(), Dict("name"->"paolo", "age"-> 40)))
    whenReduced(s.serialize(message3)) { json =>
      json mustBe """[16,341284,{},"myapp.topic",[],{"name":"paolo","age":40}]"""
    }

    val message4 = Publish(341284, options = Dict(), "myapp.topic", Payload(List("paolo", true), Dict("age" -> 40)))
    whenReduced(s.serialize(message4)) { json =>
      json mustBe """[16,341284,{},"myapp.topic",["paolo",true],{"age":40}]"""
    }
  }

  it should "serialize Published object message" in {
    val message = Published(713845233, 5512315)
    whenReduced(s.serialize(message)) { json =>
      json mustBe """[17,713845233,5512315]"""
    }
  }

  it should "serialize Subscribe object message" in {
    val message = Subscribe(1, options = Dict(), "myapp.topic")
    whenReduced(s.serialize(message)) { json =>
      json mustBe """[32,1,{},"myapp.topic"]"""
    }
  }

  it should "serialize Subscribed object message" in {
    val message = Subscribed(713845233, 5512315)
    whenReduced(s.serialize(message)) { json =>
      json mustBe """[33,713845233,5512315]"""
    }
  }

  it should "serialize Unsubscribe object message" in {
    val message = Unsubscribe(85346237, 984348843)
    whenReduced(s.serialize(message)) { json =>
      json mustBe """[34,85346237,984348843]"""
    }
  }

  it should "serialize Unsubscribed object message" in {
    val message = Unsubscribed(85346237)
    whenReduced(s.serialize(message)) { json =>
      json mustBe """[35,85346237]"""
    }
  }

  it should "serialize Event object message" in {
    pending
    val message1 = Event(713845233, 5512315)
    whenReduced(s.serialize(message1)) { json =>
      json mustBe """[36,713845233,5512315,{}]"""
    }

    val message2 = Event(713845233, 5512315, Dict(), Payload(List("paolo", 40, true)))
    whenReduced(s.serialize(message2)) { json =>
      json mustBe s"""[36,713845233,5512315,{},["paolo",40,true]]"""
    }

    val message3 = Event(713845233, 5512315, Dict(), Payload(List(), Dict("arg0" -> "paolo", "age" -> 40, "arg2" -> true)))
    whenReduced(s.serialize(message3)) { json =>
      json mustBe """[36,713845233,5512315,{},[],{"arg0":"paolo","age":40,"arg2":true}]"""
    }
  }

  it should "serialize Register object message" in {
    val message = Register(1, Dict(), "myapp.procedure")
    whenReduced(s.serialize(message)) { json =>
      json mustBe """[64,1,{},"myapp.procedure"]"""
    }
  }

  it should "serialize Registered object message" in {
    val message = Registered(713845233, 5512315)
    whenReduced(s.serialize(message)) { json =>
      json mustBe """[65,713845233,5512315]"""
    }
  }

  it should "serialize Unregister object message" in {
    val message = Unregister(85346237, 43784343)
    whenReduced(s.serialize(message)) { json =>
      json mustBe """[66,85346237,43784343]"""
    }
  }

  it should "serialize Unregistered object message" in {
    val message = Unregistered(85346237)
    whenReduced(s.serialize(message)) { json =>
      json mustBe """[67,85346237]"""
    }
  }

  it should "serialize Call object message" in {
    pending
    val message1 = Call(341284, options = Dict(), "myapp.procedure")
    whenReduced(s.serialize(message1)) { json =>
      json mustBe """[48,341284,{},"myapp.procedure"]"""
    }

    val message2 = Call(341284, options = Dict(), "myapp.procedure", Payload(List("paolo", 40, true)))
    whenReduced(s.serialize(message2)) { json =>
      json mustBe """[48,341284,{},"myapp.procedure",["paolo",40,true]]"""
    }

    val message3 = Call(341284, options = Dict(), "myapp.procedure", Payload(List(), Dict("name" -> "paolo", "age" -> 40)))
    whenReduced(s.serialize(message3)) { json =>
      json mustBe """[48,341284,{},"myapp.procedure",[],{"name":"paolo","age":40}]"""
    }

    val message4 = Call(341284, options = Dict(), "myapp.procedure", Payload(List("paolo", true), Dict("age" -> 40)))
    whenReduced(s.serialize(message4)) { json =>
      json mustBe """[48,341284,{},"myapp.procedure",["paolo",true],{"age":40}]"""
    }
  }

  
  it should "serialize Invocation object message" in {
    pending
    val message1 = Invocation(9, 431, details = Dict())
    whenReduced(s.serialize(message1)) { json =>
      json mustBe """[68,9,431,{}]"""
    }

    val message2 = Invocation(9, 431, details = Dict(), Payload(List(null, "paolo", 40, true)))
    whenReduced(s.serialize(message2)) { json =>
      json mustBe """[68,9,431,{},[null,"paolo",40,true]]"""
    }

    val message3 = Invocation(9, 431, details = Dict(), Payload(List(), Dict("name" -> "paolo", "age" -> 40)))
    whenReduced(s.serialize(message3)) { json =>
      json mustBe """[68,9,431,{},[],{"name":"paolo","age":40}]"""
    }

    val message4 = Invocation(9, 431, details = Dict(), Payload(List("paolo", true), Dict("age" -> 40)))
    whenReduced(s.serialize(message4)) { json =>
      json mustBe """[68,9,431,{},["paolo",true],{"age":40}]"""
    }
  }

  it should "serialize Yield object message" in {
    pending
    val message1 = Yield(9, options = Dict())
    whenReduced(s.serialize(message1)) { json =>
      json mustBe """[70,9,{}]"""
    }

    val message2 = Yield(9, options = Dict(), Payload(List(None, "paolo", 40, Some(true))))
    whenReduced(s.serialize(message2)) { json =>
      json mustBe """[70,9,{},[null,"paolo",40,true]]"""
    }

    val message3 = Yield(9, options = Dict(), Payload(List(), Dict("name" -> "paolo", "age" -> 40)))
    whenReduced(s.serialize(message3)) { json =>
      json mustBe """[70,9,{},[],{"name":"paolo","age":40}]"""
    }

    val message4 = Yield(9, options = Dict(), Payload(List("paolo", true), Dict("age" -> 40)))
    whenReduced(s.serialize(message4)) { json =>
      json mustBe """[70,9,{},["paolo",true],{"age":40}]"""
    }
  }

  it should "serialize Result object message" in {
    pending
    val message1 = Result(9, details = Dict())
    whenReduced(s.serialize(message1)) { json =>
      json mustBe """[50,9,{}]"""
    }

    val message2 = Result(9, details = Dict(), Payload(List("paolo", 40, true)))
    whenReduced(s.serialize(message2)) { json =>
      json mustBe """[50,9,{},["paolo",40,true]]"""
    }

    val message3 = Result(9, details = Dict(), Payload(List(), Dict("name" -> "paolo", "age" -> 40)))
    whenReduced(s.serialize(message3)) { json =>
      json mustBe """[50,9,{},[],{"name":"paolo","age":40}]"""
    }

    val message4 = Result(9, details = Dict(), Payload(List("paolo", true), Dict("age" -> 40)))
    whenReduced(s.serialize(message4)) { json =>
      json mustBe """[50,9,{},["paolo",true],{"age":40}]"""
    }
  }
  
  def whenReduced(source: String)(testCode: (String) => Unit): Unit = {
    // whenReady(source.runReduce(_ + _)) { text =>
    testCode(source)
    // }
  }

  def source[A](x: A): A = identity(x)
}

