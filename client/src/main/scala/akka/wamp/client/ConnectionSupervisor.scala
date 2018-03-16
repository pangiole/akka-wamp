/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.md', which is part of this source code package.
 */

package akka.wamp.client

import java.net.URI

import akka.actor.Actor
import akka.wamp.BackoffOptions
import akka.wamp.client.ConnectionSupervisor.SpawnWorker

import scala.concurrent.Promise

/* Spawns and supervises all connector actors */
private[client] class ConnectionSupervisor extends Actor {

  override def receive: Receive = {
    case SpawnWorker(address, format, options, promise) =>
      context.actorOf(ConnectionWorker.props(address, format, options, promise))
  }
}


/* Spawns and supervises all connector actors */
private[client] object ConnectionSupervisor {

  /* Commands new connector to be spawned */
  case class SpawnWorker(
    address: URI,
    format: String,
    options: BackoffOptions,
    promise: Promise[Connection]
  )

  // TODO case class SpawnWorker(endpoint: Endpoint)
}
