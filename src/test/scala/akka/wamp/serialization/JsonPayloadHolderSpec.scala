package akka.wamp.serialization

import akka.wamp.messages.PayloadHolder

class JsonPayloadHolderSpec 
  extends DeserializerSpec 
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
      val txt = source(json).concat(source(""",[null,123,12345678890,1.45,"string",true,["elem1"],{"key1":"value1"}]]"""))
      val message = s.deserialize(txt)
      message match {
        case holder: PayloadHolder =>
          whenReady(holder.payload.parsed) { content =>
            content.args(0) mustBe None
            content.args(1) mustBe a[java.lang.Integer]
            content.args(2) mustBe a[java.lang.Long]
            content.args(3) mustBe a[java.lang.Double]
            content.args(4) mustBe a[java.lang.String]
            content.args(5) mustBe a[java.lang.Boolean]
            content.args(6) mustBe a[scala.collection.immutable.List[_]]
            content.args(7) mustBe a[scala.collection.immutable.Map[_, _]]
          }
        case msg =>
          fail(s"Unexpected $msg")
      }
    }

    
    /*it should "hold unparsed and parsed (as dictionary) text payload" in {
      val txt = source(json).concat(source(""",[null,"paolo",40,true],{"height":1.63,"1":"pietro"}]"""))
      val message = s.deserialize(txt)
      message match {
        case holder: PayloadHolder =>
          holder.payload match {
            case p: TextLazyPayload =>
              whenReduced(p.unparsed) { text =>
                text mustBe """[null,"paolo",40,true],{"height":1.63,"1":"pietro"}]"""
              }
              whenReady(p.parsed) { content =>
                content.args mustBe List(None, "paolo", 40,  true)
                content.kwargs mustBe Map("height"->1.63d, "1"->"pietro")
              }
            case p =>
              fail(s"Unexpected $p")
          }
        case msg =>
          fail(s"Unexpected $msg")
      }
    }*/

    
    it should "hold huge unparsed text payload as Akka Stream Source" in {
      pending
      // TODO file an issue for huge unparsed text payload (consider pull request #3)
    }
  }
}

