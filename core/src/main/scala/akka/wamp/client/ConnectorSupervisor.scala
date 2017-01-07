/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.md', which is part of this source code package.
 */

package akka.wamp.client

import java.net.URI

import akka.actor.SupervisorStrategy.{Restart, Stop}
import akka.actor.{Actor, ActorInitializationException, ActorKilledException, DeathPactException, OneForOneStrategy, SupervisorStrategy}
import akka.wamp.client.ConnectorSupervisor.SpawnConnector

import scala.concurrent.Promise
import scala.concurrent.duration.FiniteDuration

/* Spawns and supervises all connector actors */
private[client] class ConnectorSupervisor extends Actor {

  override def receive: Receive = {
    case SpawnConnector(url, format, options, promise) =>
      context.actorOf(Connector.props(url, format, options, promise))
  }

  override def supervisorStrategy: SupervisorStrategy =
    OneForOneStrategy()
      {
        case _: ActorInitializationException => Stop
        case _: ActorKilledException         => Stop
        case _: DeathPactException           => Stop
        case _: Exception                    => Restart
      }
    
}


/* Spawns and supervises all connector actors */
private[client] object ConnectorSupervisor {

  /* Commands new connector to be spawned */
  case class SpawnConnector(
    uri: URI,
    format: String,
    options: BackoffOptions,
    promise: Promise[Connection]
  )

  /* Are the backoff options.*/
  private[client] class BackoffOptions(
    val minBackoff: FiniteDuration,
    val maxBackoff: FiniteDuration,
    val randomFactor: Double
  )
}
