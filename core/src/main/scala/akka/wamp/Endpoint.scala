package akka.wamp

import java.net.URI

import com.typesafe.config.Config

/**
  * An endpoint which provides WAMP services
  *
  * @param name is the name
  * @param address is the address (e.g. wss://host.net:8433/wamp)
  * @param format is the message format (e.g. json)
  */
class Endpoint private (
  val name: String,
  val address: URI,
  val format: String
) {
  def toTuple = (name, address, format)
}

/**
  * The factory of endpoints
  */
object Endpoint {
  /**
    * Read endpoint configuration
    *
    * @param name is the name
    * @param config is the configuration object
    * @return
    */
  def fromConfig(config: Config, name: String): Endpoint = {
    val c = config
      .getConfig(s"endpoint.$name")
      .withFallback(
        config.getConfig("endpoint.default")
      )
    new Endpoint(
      name,
      c.getURI("address"),
      c.getString("format")
    )
  }
}
