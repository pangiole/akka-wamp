package akka.wamp


import akka.actor._
import akka.io.IO
import akka.wamp.messages._


/**
  * WAMP Extension for Akka’s IO layer.
  *
  * For a full description of the design and philosophy behind this IO
  * implementation please refer to <a href="http://doc.akka.io/">the Akka online documentation</a>.
  *
  * In order to open an outbound connection send a [[Connect]] message
  * to the [[WampExtension#manager]].
  *
  * In order to start listening for inbound connections send a [[Bind]]
  * message to the [[WampExtension#manager]].
  */
object Wamp extends ExtensionId[WampExtension] with ExtensionIdProvider {
  
  /**
    * Returns the canonical ExtensionId for this Extension
    */
  override def lookup(): ExtensionId[_ <: Extension] = Wamp

  /**
    * Is used by Akka to instantiate the Extension identified by this ExtensionId,
    * internal use only.
    */
  override def createExtension(system: ExtendedActorSystem): WampExtension = new WampExtension(system)
}



class WampExtension(system: ExtendedActorSystem) extends IO.Extension {
  val manager = system.actorOf(ExtensionManager.props(), name = "IO-Wamp")
}
