/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.md', which is part of this source code package.
 */

package akka.wamp.client.japi

import akka.actor.{AbstractActor, AbstractLoggingActor}
import akka.wamp.client.ClientActor

abstract class AbstractClientActor extends AbstractActor with ClientActor


abstract class AbstractClientLoggingActor extends AbstractLoggingActor with ClientActor

