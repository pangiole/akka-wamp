/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.md', which is part of this source code package.
 */

package akka.wamp

import akka.testkit.TestKit

trait RouterAddress { this: TestKit =>

  val config = system.settings.config

  def url(port: Int) = {
    val trsnprt = config.getConfig("akka.wamp.router.transport.default")
    val protocol = trsnprt.getString("protocol")
    val iface = trsnprt.getString("iface")
    val upath = trsnprt.getString("upath")
    s"$protocol://$iface:$port/$upath"
  }

}
