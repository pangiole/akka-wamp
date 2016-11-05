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

//  "A Publish object message" should behave like argumentsConveyor(s, prefix="""[16,9007199254740992,{"acknowledge":true},"myapp.topic"""")
//
//  "An Event object message" should behave like argumentsConveyor(s, prefix="""[36,1,1,{}""")
//
//  "An Invocation object message" should behave like argumentsConveyor(s, prefix="""[68,1,1,{}""")
//
//  "A Call object message" should behave like argumentsConveyor(s, prefix="""[48,1,{},"myapp.procedure"""")
//
//  "A Yield object message" should behave like argumentsConveyor(s, prefix="""[70,1,{}""")
//
//  "A Result object message" should behave like argumentsConveyor(s, prefix="""[50,1,{}""")
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
          whenReady(c.args) { a =>
            a mustBe empty
            whenReady(c.kwargs) { a =>
              a mustBe empty
            }
          }
        }
        case msg => fail(s"Unexpected $msg")
      }
    }


    it should "convey arguments deserializable to a provided user type" in {
      pending
      val json = single(prefix).concat(single(""" ,  [],{"name":"paolo", "age": 99, "male":   true}]"""))
      val message = s.deserialize(json)
      message match {
        case c: DataConveyor =>
          whenReady(c.kwargs[UserType]) { user =>
            user.name mustBe "paolo"
            user.age mustBe 99
            user.male mustBe true
          }
        case msg =>
          fail(s"Unexpected $msg")
      }
    }


    it should "convey arguments deserializable to default types" in {
      pending
      //                                         0    1         2           3    4        5   6     7                                                     8
      val json = single(prefix).concat(single(""",  [null,2147483647,2147483648,1.45,"string",true,false,[null,2147483647,2147483648,1.45,"string",true,false],{"key0":null,"key1":2147483647,"key2":2147483648,"key3":1.45,"key4":"string","key5":true,"key6":false}],{"arg0":null,"arg1":2147483647,"arg2":2147483648,"arg3":1.45,"arg4":"string","arg5":true,"arg6":false,"arg7":[null,2147483647,2147483648,1.45,"string",true,false],"arg8":{"key0":null,"key1":2147483647,"key2":2147483648,"key3":1.45,"key4":"string","key5":true,"key6":false}}]"""))
      val message = s.deserialize(json)
      message match {
        case c: DataConveyor =>
          whenReady(c.args) { args =>
            args must have size (9)
            // TODO args(0) mustBe (null)
            args(1) mustBe a[java.lang.Integer]
            args(1) mustBe 2147483647
            args(2) mustBe a[java.lang.Long]
            args(2) mustBe 2147483648L
            args(3) mustBe a[java.lang.Double]
            args(3) mustBe 1.45
            args(4) mustBe a[java.lang.String]
            args(4) mustBe "string"
            args(5) mustBe a[java.lang.Boolean]
            args(5) mustBe true
            args(6) mustBe a[java.lang.Boolean]
            args(6) mustBe false
            args(7) mustBe a[scala.collection.immutable.List[_]]
            args(7) mustBe List(null, 2147483647, 2147483648L, 1.45, "string", true, false)
            args(8) mustBe a[scala.collection.immutable.Map[_, _]]
            args(8) mustBe Map("key0" -> null, "key1" -> 2147483647, "key2" -> 2147483648L, "key3" -> 1.45, "key4" -> "string", "key5" -> true, "key6" -> false)
          }
          whenReady(c.kwargs) { kwargs =>
            kwargs must have size(9)
            // TODO kwargs("arg0") mustBe null
            kwargs("arg1") mustBe a[java.lang.Integer]
            kwargs("arg1") mustBe 2147483647
            kwargs("arg2") mustBe a[java.lang.Long]
            kwargs("arg2") mustBe 2147483648L
            kwargs("arg3") mustBe a[java.lang.Double]
            kwargs("arg3") mustBe 1.45
            kwargs("arg4") mustBe a[java.lang.String]
            kwargs("arg4") mustBe "string"
            kwargs("arg5") mustBe a[java.lang.Boolean]
            kwargs("arg5") mustBe true
            kwargs("arg6") mustBe a[java.lang.Boolean]
            kwargs("arg6") mustBe false
            kwargs("arg7") mustBe a[scala.collection.immutable.List[_]]
            kwargs("arg7") mustBe List(null,2147483647,2147483648L,1.45,"string",true,false)
            kwargs("arg8") mustBe a[scala.collection.immutable.Map[_, _]]
            kwargs("arg8") mustBe Map("key0"->null,"key1"->2147483647,"key2"->2147483648L,"key3"->1.45,"key4"->"string","key5"->true,"key6"->false)
          }
        case msg =>
          fail(s"Unexpected $msg")
      }
    }


    it should "convey unparsed textual payload" in {
      pending
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

