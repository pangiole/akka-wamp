/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.md', which is part of this source code package.
 */

package akka.wamp.serialization

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.testkit.TestKit
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{FlatSpecLike, MustMatchers}

class ChopOffFlowSpec extends TestKit( ActorSystem("test"))
  with FlatSpecLike with MustMatchers
  with ScalaFutures with IntegrationPatience {

  implicit val mat = ActorMaterializer()

  "The flow" should "chop off a given number of bytes" in {
    val flowUnderTest = new ChopOff/*[String]*/(8)

    val elements = Seq(
      "This is the first element emitted by the source",
      "and this will be the second one"
    )

    val future =
      Source.fromIterator(() => elements.toIterator)
        .via(flowUnderTest)
        .runWith(Sink.fold(Seq.empty[String])(_ :+ _))

    whenReady(future) { result =>
      result mustBe Seq(
        /*"This is "*/"the first element emitted by the source",
        "and this will be the second one"
      )
    }
  }

  // TODO add more test scenarios
}
