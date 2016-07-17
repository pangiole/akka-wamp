package akka.wamp

import akka.wamp.client._
import akka.wamp.router._

/**
  * A Peer could be either a [[Client]] or a [[Router]]
  *  - it must implement one [[Role]], and
  *  - may implement more [[Role]]s.
  */
trait Role
