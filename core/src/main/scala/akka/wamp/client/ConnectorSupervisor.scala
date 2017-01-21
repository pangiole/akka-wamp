/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.md', which is part of this source code package.
 */

package akka.wamp.client

import java.net.URI

import akka.actor.SupervisorStrategy.{Restart, Stop}
import akka.actor.{Actor, ActorInitializationException, ActorKilledException, DeathPactException, OneForOneStrategy, SupervisorStrategy}
import akka.wamp.BackoffOptions
import akka.wamp.client.ConnectorSupervisor.SpawnConnector

import scala.concurrent.Promise

/* Spawns and supervises all connector actors */
private[client] class ConnectorSupervisor extends Actor {

  override def receive: Receive = {
    case SpawnConnector(address, format, options, promise) =>
      context.actorOf(Connector.props(address, format, options, promise))
  }
}


/* Spawns and supervises all connector actors */
private[client] object ConnectorSupervisor {

  /* Commands new connector to be spawned */
  case class SpawnConnector(
    address: URI,
    format: String,
    options: BackoffOptions,
    promise: Promise[Connection]
  )

}
