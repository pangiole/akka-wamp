/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.md', which is part of this source code package.
 */

/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.md', which is part of this source code package.
 */

package akka.wamp.client.japi

import akka.wamp.messages.{Unregistered => UnregisteredDelegate}

/**
  * Is the unregistered acknowledge
  */
class Unregistered(delegate: UnregisteredDelegate)
