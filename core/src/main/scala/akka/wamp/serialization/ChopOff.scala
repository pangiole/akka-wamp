/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.md', which is part of this source code package.
 */

package akka.wamp.serialization

import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}


/**
  * INTERNAL API
  *
  * Is a custom Akka Stream Graph Stage able to drop the given number of atoms
  * from the first elements emitted by the upstream stages.
  */
private[serialization] class ChopOff(n: Int) extends GraphStage[FlowShape[String, String]] {
  val out = Outlet[String]("ChopOff")
  val in = Inlet[String]("ChopOff")

  override def shape: FlowShape[String, String] = FlowShape(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = {
    new GraphStageLogic(shape) {
      // all mutable state MUST be inside the GraphStageLogic
      var count = n

      setHandler(in, new InHandler {
        override def onPush() = {
          val elem = grab(in)
          val len = elem.length
          if (len > count) {
            // len   ..................................
            // count .....................|
            val chopped = elem.drop(count)
            count = 0
            push(out, chopped)
          }
          else {
            // len   ...............
            // count .....................|
            count = count - len
            pull(in)
          }
        }
      })
      setHandler(out, new OutHandler {
        override def onPull() = {
          pull(in)
        }
      })
    }
  }
}
