/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.md', which is part of this source code package.
 */

package akka.wamp.messages

import akka.stream.scaladsl._
import akka.wamp.serialization.{JsonSerialization, SerializingBaseSpec, TextLazyPayload}
import com.fasterxml.jackson.core.JsonFactory


class JsonMessageSpec
  extends SerializingBaseSpec
    with JsonMessageBehaviours
{

  val s = new JsonSerialization(new JsonFactory())

  "An Error object message" should behave like argumentsConveyor(s, prefix="""[8,34,9007199254740992,{},"wamp.error.no_such_subscription"""")

  "A Publish object message" should behave like argumentsConveyor(s, prefix="""[16,9007199254740992,{"acknowledge":true},"myapp.topic"""")

  "An Event object message" should behave like argumentsConveyor(s, prefix="""[36,1,1,{}""")

  "An Invocation object message" should behave like argumentsConveyor(s, prefix="""[68,1,1,{}""")

  "A Call object message" should behave like argumentsConveyor(s, prefix="""[48,1,{},"myapp.procedure"""")

  "A Yield object message" should behave like argumentsConveyor(s, prefix="""[70,1,{}""")

  "A Result object message" should behave like argumentsConveyor(s, prefix="""[50,1,{}""")
}


case class UserType(name: String, age: Int, male: Boolean)


trait JsonMessageBehaviours { this: JsonMessageSpec =>

  private def whenReduced(source: Source[String, _])(testCode: String => Unit): Unit = {
    whenReady(source.runReduce(_ + _)) { text =>
      testCode(text)
    }
  }


  def argumentsConveyor(s: JsonSerialization, prefix: String) = {

    it should "convey no arguments" in {
      val json = single(prefix).concat(single("  ]"))
      val message = s.deserialize(json)
      message match {
        case c: DataConveyor => {
          c.args mustBe empty
          c.kwargs mustBe empty
        }
        case msg => fail(s"Unexpected $msg")
      }
    }


    it should "convey arguments deserializable to a provided user type" in {
      val json = single(prefix).concat(single(""" ,  [],{"name":"paolo", "age": 99, "male":   true}]"""))
      val message = s.deserialize(json)
      message match {
        case c: DataConveyor =>
          val user = c.kwargs[UserType]
          user.name mustBe "paolo"
          user.age mustBe 99
          user.male mustBe true
        case msg =>
          fail(s"Unexpected $msg")
      }
    }


    it should "convey arguments deserializable to default types" in {
      //                                         0    1         2           3    4        5   6     7                                                     8
      val json = single(prefix).concat(single(""",  [null,2147483647,2147483648,1.45,"string",true,false,[null,2147483647,2147483648,1.45,"string",true,false],{"key0":null,"key1":2147483647,"key2":2147483648,"key3":1.45,"key4":"string","key5":true,"key6":false}],{"arg0":null,"arg1":2147483647,"arg2":2147483648,"arg3":1.45,"arg4":"string","arg5":true,"arg6":false,"arg7":[null,2147483647,2147483648,1.45,"string",true,false],"arg8":{"key0":null,"key1":2147483647,"key2":2147483648,"key3":1.45,"key4":"string","key5":true,"key6":false}}]"""))
      val message = s.deserialize(json)
      message match {
        case c: DataConveyor =>
          c.args must have size (9)
          // TODO c.args(0) mustBe (null)
          c.args(1) mustBe a[java.lang.Integer]
          c.args(1) mustBe 2147483647
          c.args(2) mustBe a[java.lang.Long]
          c.args(2) mustBe 2147483648L
          c.args(3) mustBe a[java.lang.Double]
          c.args(3) mustBe 1.45
          c.args(4) mustBe a[java.lang.String]
          c.args(4) mustBe "string"
          c.args(5) mustBe a[java.lang.Boolean]
          c.args(5) mustBe true
          c.args(6) mustBe a[java.lang.Boolean]
          c.args(6) mustBe false
          c.args(7) mustBe a[scala.collection.immutable.List[_]]
          c.args(7) mustBe List(null, 2147483647, 2147483648L, 1.45, "string", true, false)
          c.args(8) mustBe a[scala.collection.immutable.Map[_, _]]
          c.args(8) mustBe Map("key0" -> null, "key1" -> 2147483647, "key2" -> 2147483648L, "key3" -> 1.45, "key4" -> "string", "key5" -> true, "key6" -> false)

          c.kwargs must have size(9)
          // TODO c.kwargs("arg0") mustBe null
          c.kwargs("arg1") mustBe a[java.lang.Integer]
          c.kwargs("arg1") mustBe 2147483647
          c.kwargs("arg2") mustBe a[java.lang.Long]
          c.kwargs("arg2") mustBe 2147483648L
          c.kwargs("arg3") mustBe a[java.lang.Double]
          c.kwargs("arg3") mustBe 1.45
          c.kwargs("arg4") mustBe a[java.lang.String]
          c.kwargs("arg4") mustBe "string"
          c.kwargs("arg5") mustBe a[java.lang.Boolean]
          c.kwargs("arg5") mustBe true
          c.kwargs("arg6") mustBe a[java.lang.Boolean]
          c.kwargs("arg6") mustBe false
          c.kwargs("arg7") mustBe a[scala.collection.immutable.List[_]]
          c.kwargs("arg7") mustBe List(null,2147483647,2147483648L,1.45,"string",true,false)
          c.kwargs("arg8") mustBe a[scala.collection.immutable.Map[_, _]]
          c.kwargs("arg8") mustBe Map("key0"->null,"key1"->2147483647,"key2"->2147483648L,"key3"->1.45,"key4"->"string","key5"->true,"key6"->false)

        case msg =>
          fail(s"Unexpected $msg")
      }
    }


    it should "convey unparsed textual payload" in {
      val suffix = single(",???]")
      val json = single(prefix).concat(suffix)
      val message = s.deserialize(json)
      message match {
        case c: DataConveyor => c.payload match {
          case p: TextLazyPayload =>
            whenReduced(suffix) { original =>
              whenReduced(p.unparsed) { unparsed =>
                unparsed mustBe original
              }
            }
          case msg =>
            fail(s"Unexpected $msg")
        }
        case msg =>
          fail(s"Unexpected $msg")
      }
    }
  }
}

