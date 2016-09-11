package akka.wamp

import akka.wamp.messages.Event
import akka.wamp.serialization.Payload


package object client {
  
  type ArgumentsHandler = (List[Any]) => Unit
  
  type PayloadHandler = (Payload) => ArgumentsHandler
  
  type EventHandler = Event => Unit
}
