/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.md', which is part of this source code package.
 */

/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.md', which is part of this source code package.
 */

package akka.wamp.client

import akka.actor.{AbstractActor, AbstractLoggingActor}

abstract class AbstractClientActor extends AbstractActor with ClientActor


abstract class AbstractClientLoggingActor extends AbstractLoggingActor with ClientActor

