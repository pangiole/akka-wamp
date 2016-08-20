package akka.wamp

import akka.wamp.messages.Event


package object client {
  type EventHandler = (Event => Unit)
}
