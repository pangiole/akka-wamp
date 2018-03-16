/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.md', which is part of this source code package.
 */

import java.net.URI

import akka.actor._
import akka.io._
import akka.wamp._
import akka.wamp.client._
import akka.wamp.messages._

import scala.concurrent.duration._

/**
  * Created by paolo on 19/01/2017.
  */
object ActorsScalaClientApp extends App {

  val system = ActorSystem("examples")
  system.actorOf(Props[ConnectionWorker], name = "connector")


  class ConnectionWorker extends Actor with ActorLogging with ClientActor {
    import ConnectionWorker._

    var attempts = 0
    var sessionId: Long = _
    var gen: SessionScopedIdGenerator = _
    var requestId: Id = _

    override def preStart(): Unit = {
      self ! AttemptConnection
    }

    override def receive(): Receive = {
      case AttemptConnection =>
        if (attempts < MaxAttempts) {
          attempts = attempts + 1
          log.info("Connection attempt #{} to {}", attempts, uri)
          IO(Wamp) ! Connect(uri, "json")
        }
        else {
          log.warning("Max connection attempts reached!")
          self ! PoisonPill
        }

      case sig @ CommandFailed(cmd: Connect, ex) =>
        log.warning(ex.getMessage)
        scheduler.scheduleOnce(1.second, self, AttemptConnection)

      case sig @ Connected(conn, _, _) =>
        log.info("Connected {}", conn)
        attempts = 0
        context become connected(conn)
        conn ! Hello("default")
    }



    def connected(conn: ActorRef): Receive = {
      case sig @ Disconnected =>
        log.warning("Disconnected")
        self ! AttemptConnection

      case msg @ Abort(details, reason) =>
        log.warning(msg.toString)
        self ! PoisonPill

      case msg @ Welcome(sessionId, details) =>
        this.sessionId = sessionId
        this.gen = new SessionScopedIdGenerator
        log.info("Session #{} open", sessionId)
        context become open(conn, sessionId)
        requestId = gen.nextId()
        conn ! Subscribe(requestId, Dict(), "myapp.topic1")
    }


    var subscriptionId: Id = _
    def open(conn: ActorRef, sessionId: Id): Receive = {
      case Subscribed(reqId, subId)  =>
        if (reqId == requestId)
          subscriptionId = subId

      case evt @ Event(subId, _, _, _) =>
        if (subId == subscriptionId)
          if (evt.args(0).toString == "Everybody out!") {
            context become handleDisconnecting
            conn ! Disconnect
          }

      case sig @ Disconnected =>
        log.info("Disconnected")
        self ! PoisonPill
    }


    def handleDisconnecting: Receive = {
      case sig @ Disconnected =>
        log.info(s"Disconnected")
        self ! PoisonPill
    }

    override def postStop(): Unit = {
      system.terminate().map(t => System.exit(0))
    }
  }

  object ConnectionWorker {
    val uri = new URI("ws://localhost:8080/wamp")
    val MaxAttempts = 8
    case object AttemptConnection
  }
}
