package akka.wamp.client

import akka.actor.Actor._
import akka.wamp._
import akka.wamp.messages._
import akka.wamp.serialization.Payload

import scala.collection.mutable
import scala.concurrent.{Future, Promise}

/**
  * A caller is a client that calls procedures and expects results
  */
trait Caller { this: Session =>
  import Caller._

  /**
    * The map of pending calls which collects those calls waiting for a result
    */
  private val pendingCalls: mutable.Map[RequestId, PendingCall] = mutable.Map()
  // TODO how to remove those calls that could be never replied?
  
  /**
    * Call a procedure so that the given handler will be 
    * executed on invocations.
    *
    * A callee announces the availability of an endpoint implementing a
    * procedure with a dealer by sending a REGISTER message:
    *
    * {{{
    *   ,------.          ,------.          ,------.
    *   |Caller|          |Dealer|          |Callee|
    *   `--+---'          `--+---'          `--+---'
    *      |       CALL      |                 |
    *      | ---------------->                 |
    *      |                 |                 |
    *      |                 |    INVOCATION   |
    *      |                 | ---------------->
    *      |                 |                 |
    *      |                 |  YIELD or ERROR |
    *      |                 | <----------------
    *      |                 |                 |
    *      | RESULT or ERROR |                 |
    *      | <----------------                 |
    *   ,--+---.          ,--+---.          ,--+---.
    *   |Caller|          |Dealer|          |Callee|
    *   `------'          `------'          `------'
    * }}}
    *
    * @param procedure is the procedure the callee wants to register
    * @param payload ???
    * @return the (future of) result
    */
  def call(procedure: Uri, payload: Option[Payload] = None): Future[Result] = {
    withPromise[Result] { promise =>
      val msg = Call(requestId = nextId(), Call.defaultOptions, procedure, payload)
      pendingCalls += (msg.requestId -> new PendingCall(msg, promise))
      connection ! msg
    }
  }
  

  private[client] def handleResults: Receive = {
    case msg @ Result(requestId, details, payload) =>
      log.debug("<-- {}", msg)
      pendingCalls.get(requestId).map { pending =>
        pendingCalls -= requestId
        pending.promise.success(msg)
      }
      
    case msg @ Error(Result.tpe, requestId, _, error, _) =>
      log.debug("<-- {}", msg)
      pendingCalls.get(requestId).map { pending =>
        pendingCalls -= requestId
        pending.promise.failure(new SessionException(error))
      }
  }
}

object Caller {
  private class PendingCall(val message: Call, val promise: Promise[Result])  
}


