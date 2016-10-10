package akka.wamp.serialization

import akka.wamp.messages.PayloadHolder

class JsonPayloadHolderSpec 
  extends SerializingBaseSpec 
    with JsonPayloadHolderBehaviours 
{
  
  val s = new JsonSerialization

  "An Error object message" should behave like jsonPayloadHolder(s,"""[8,34,9007199254740992,{},"wamp.error.no_such_subscription"""")

  "A Publish object message" should behave like jsonPayloadHolder(s,"""[16,9007199254740992,{"acknowledge":true},"myapp.topic"""")

  "An Event object message" should behave like jsonPayloadHolder(s,"""[36,1,1,{}""")

  "An Invocation object message" should behave like jsonPayloadHolder(s,"""[68,1,1,{}""")

  "A Call object message" should behave like jsonPayloadHolder(s,"""[48,1,{},"myapp.procedure"""")

  "A Yield object message" should behave like jsonPayloadHolder(s,"""[70,1,{}""")

  "A Result object message" should behave like jsonPayloadHolder(s,"""[50,1,{}""")
}



trait JsonPayloadHolderBehaviours { this: JsonPayloadHolderSpec =>

  private def whenReduced(source: String)(testCode: (String) => Unit): Unit = {
    // whenReady(source.runReduce(_ + _)) { text =>
    testCode(source)
    // }
  }

  def jsonPayloadHolder(s: JsonSerialization, json: String) = {

    it should "hold empty payload" in {
      val txt = source(json).concat(source("]"))
      val message = s.deserialize(txt)
      message match {
        case holder: PayloadHolder => assert(true)
        case msg => fail(s"Unexpected $msg")
      }
    }

    it should "hold payload parsed applying default Jackson data types binding" in {
      //                                         0    1   2           3    4        5    6          7 
      val txt = source(json).concat(source(""",[null,123,12345678890,1.45,"string",true,["elem1"],{"key1":"value1"}],{"arg0":null,"arg1":123,"arg2":12345678890,"arg3":1.45,"arg4":"string","arg5":true,"arg6":["elem1"],"arg7":{"key1":"value1"}}]"""))
      val message = s.deserialize(txt)
      message match {
        case holder: PayloadHolder =>
          whenReady(holder.payload.parsed) { content =>
            content.args must have size(8)
            content.args(0) mustBe None
            content.args(1) mustBe a[java.lang.Integer]
            content.args(2) mustBe a[java.lang.Long]
            content.args(3) mustBe a[java.lang.Double]
            content.args(4) mustBe a[java.lang.String]
            content.args(5) mustBe a[java.lang.Boolean]
            content.args(6) mustBe a[scala.collection.immutable.List[_]]
            content.args(7) mustBe a[scala.collection.immutable.Map[_, _]]

            content.kwargs must have size(8)
            content.kwargs("arg0") mustBe None
            content.kwargs("arg1") mustBe a[java.lang.Integer]
            content.kwargs("arg2") mustBe a[java.lang.Long]
            content.kwargs("arg3") mustBe a[java.lang.Double]
            content.kwargs("arg4") mustBe a[java.lang.String]
            content.kwargs("arg5") mustBe a[java.lang.Boolean]
            content.kwargs("arg6") mustBe a[scala.collection.immutable.List[_]]
            content.kwargs("arg7") mustBe a[scala.collection.immutable.Map[_, _]]
          }
        case msg =>
          fail(s"Unexpected $msg")
      }
    }
    
    // TODO it should "hold huge unparsed text payload as Akka Stream Source" 
  }
}

