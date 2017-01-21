package akka

import akka.wamp.messages._
import com.typesafe.config.Config
import java.net.URI

import scala.concurrent.duration.{FiniteDuration, NANOSECONDS}
import scala.util.Random.{nextDouble => rnd}

/**
  * Contains classes, traits, types and functions to be used to write applications
  * based on WAMP - Web Application Messaging Protocol
  *
  * == Client ==
  *
  * Akka Wamp provides you with three distinct Client APIs in the [[wamp.client]] package
  *
  *   - Actor based
  *   - Future based
  *   - Stream based
  *
  * == Router ==
  *
  * Akka Wamp provides you with a basic router implementation in the [[wamp.router]] package
  */
package object wamp {

  /**
    * Represents a role played by a [[Peer]]
    */
  type Role = String


  /**
    * Are the roles sets
    */
  object Roles {
    /**
      * It the set of roles played by a fully capable client
      */
    val client = Set("callee", "caller", "publisher", "subscriber")
    /**
      * Is the set of roles played for a fully capable router
      */
    val router = Set("broker", "dealer")
  }


  private[wamp] type TypeCode = Int

  private[wamp] object TypeCodes {
    val all = List(
      Hello.tpe, Welcome.tpe, Abort.tpe, Goodbye.tpe,
      Error.tpe,
      Publish.tpe, Published.tpe,
      Subscribe.tpe, Subscribed.tpe,
      Unsubscribe.tpe, Unsubscribed.tpe,
      Event.tpe,
      Register.tpe, Registered.tpe,
      Unregister.tpe, Unregistered.tpe,
      Call.tpe, Invocation.tpe,
      Yield.tpe, Result.tpe
    )

    /**
      * Checks if the given type code is valid
      *
      * @param code is the type code to check against
      * @return ``true`` if the given type code is valid, ``false`` otherwise
      */
    def isValid(code: TypeCode): Boolean = all.contains(code)
  }

  /**
    * Represents a unique identifier
    */
  type Id = Long


  private[wamp] object Id {
    val min = 1L
    val max = 9007199254740992L
    def draw(): Long = min + (rnd() * (max - min + 1)).toLong
  }


  /**
    * Represents an uniform resource identifier
    */
  type Uri = String

  private[wamp] object Uri

  /**
    * Represents a dictionary.
    *
    * Instances can be created using its companion object.
    */
  type Dict = Map[String, Any]

  /**
    * Factory for [[Dict]] instances
    *
    * @see [[RichDict]]
    */
  final object Dict {

    /**
      * Creates an empty dictionary
      *
      * @return the new dictionary
      */
    def apply(): Dict = Map.empty[String, Any]

    /**
      * Creates a dictionary with given entries
      *
      * @param entries are the entries to initialize the new dictionary with
      * @return the new dictionary
      */
    def apply(entries: (String, Any)*): Dict = entries.toMap
  }

  /**
    * Implicitly wraps a [[Dict]] to provide additional utility methods
    *
    * @param dict is the dictionary to wrap
    */
  implicit class RichDict(dict: Dict) {
    /**
      * Create a new dictionary from this dictionary with the given roles added
      *
      * @param roles are the roles to add
      * @return the new dictionary
      */
    def withRoles(roles: Set[Role]): Dict = {
      withRoles(roles.toList: _*)
    }

    /**
      * Create a new dictionary from this dictionary with the given roles added
      *
      * @param roles are the roles to add
      * @return the new dictionary
      */
    def withRoles(roles: Role*): Dict = {
      dict ++ Map("roles" -> roles.sorted.map(r => (r -> Map())).toMap)
    }

    /* Create a new dictionary from this dictionary with the given agent added */
    private[wamp] def withAgent(agent: String): Dict = {
      dict + ("agent" -> agent)
    }

    /**
      * Create a new dictionary from this dictionary with the given acknowledge flag
      *
      * @param ack is the acknowledge flag to add
      * @return the new dictionary
      */
    def withAcknowledge(ack: Boolean = true): Dict = {
      dict + ("acknowledge" -> ack)
    }
  }


  /**
    * Implicitly wraps a TypeSafe [[Config]] to provide additional utility methods
    *
    * @param config
    */
  implicit class RichConfig(config: Config) {

    /**
      * Reads the given configuration path as [[URI]]
      *
      * @param path is the path expression
      * @return the configured URI
      */
    def getURI(path: String): URI = {
      new URI(config.getString(path))
    }
  }


  private[wamp] class BackoffOptions(
    val minBackoff: FiniteDuration,
    val maxBackoff: FiniteDuration,
    val randomFactor: Double
  )


  private[wamp] trait EndpointConfig {
    def endpointConfig(name: String, config: Config): (URI, String) = {
      val c = config.getConfig(s"endpoint.$name").withFallback(config.getConfig("endpoint.local"))
      (
        c.getURI("address"),
        c.getString("format")
      )
    }
  }
}
