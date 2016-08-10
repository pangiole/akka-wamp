package akka

/**
  * == akka-wamp 0.4.1 ==
  *
  * [[http://wamp-proto.org WAMP - Web Application Messaging Protocol]] implementation written in Scala with Akka
  */
package object wamp {

  /**
    * Specified message type
    */
  type Tpe = Int

  final object Tpe {
    val HELLO = 1
    val WELCOME = 2
    val ABORT = 3
    val GOODBYE = 6
    val ERROR = 8
    val PUBLISH = 16
    val PUBLISHED = 17
    val SUBSCRIBE = 32
    val SUBSCRIBED = 33
    val UNSUBSCRIBE = 34
    val UNSUBSCRIBED = 35
    val EVENT = 36
    val ALL = List(
      HELLO, WELCOME, ABORT, GOODBYE, ERROR,
      PUBLISH, PUBLISHED, SUBSCRIBE, SUBSCRIBED, UNSUBSCRIBE, UNSUBSCRIBED, EVENT
    )
    def isValid(tpy: Tpe): Boolean = ALL.contains(tpy)
  }

  /**
    * Unique identifiers being used to distinguish Sessions, Publications, 
    * Subscriptions, Registrations and Requests
    */
  type Id = Long


  final object Id {
    import scala.util.Random.{nextDouble => rnd}
    val Min = 1L
    val Max = 9007199254740992L
    def draw(): Long = Min + (rnd() * (Max - Min + 1)).toLong
    def isValid(id: Id): Boolean = id >= Min && id <= Max
  }

  
  /**
    * Uniform Resource Identifier
    */
  type Uri = String

  final object Uri {
    // strict URI check disallowing empty URI components
    val regex = "^([0-9a-zA-Z_]+\\.)*([0-9a-zA-Z_]+)$".r
    def isValid(uri: Uri) = regex.pattern.matcher(uri).matches
  }


  /**
    * Dictionary for options and details elements
    */
  type Dict = Map[String, Any]

  final object Dict {
    def apply(): Dict = Map.empty[String, Any]
    def apply(entries: (String, Any)*): Dict = entries.toMap
  }


  /* Some WAMP messages contain "Options|dict" or "Details|dict" elements.
    * This allows for future extensibility and implementations that only
    * provide subsets of functionality by ignoring unimplemented
    * attributes.  
    * TODO Keys in "Options" and "Details" MUST be of type "string" and MUST match the regular expression "[a-z][a-z0-9_]{2,}" 
    * for WAMP _predefined_ keys.  Implementations MAY use implementation-specific
    * keys that MUST match the regular expression "_[a-z0-9_]{3,}".
    * Attributes unknown to an implementation MUST be ignored.
    */

  implicit class RichDict(dict: Dict) {
    def withRoles(roles: String*): Dict = {
      dict ++ Map("roles" -> roles.map(role => (role -> Map())).toMap)
    }

    def withAgent(agent: String): Dict = {
      dict + ("agent" -> agent)
    }

    def roles: Set[String] = {
      if (dict.isDefinedAt("roles")) {
        dict("roles") match {
          case rs: Map[_, _] => rs.keySet.map(_.asInstanceOf[String])
          case _ => Set()
        }
      } else {
        Set()
      }
    }
  }


  case class Payload(val arguments: List[Any]) {
    private[wamp] def elems = {
      if (arguments.exists(_.isInstanceOf[Tuple2[_, _]])) {
        List(Nil, toMap)
      } else {
        List(arguments)
      }
    }

    def toMap: Map[String, Any] = {
      arguments.zipWithIndex.map {
        case (arg: Tuple2[_, _], _) => (arg._1.toString, arg._2)
        // TODO check that keys are of String type OR force them to become String
        case (arg, idx) => (s"arg$idx" -> arg)
      }.toMap
    }
  }

  object Payload {
    def apply(arguments: Any*): Payload = new Payload(arguments.toList)
  }

  
  private def require(condition: Boolean, message: String) =
    if (!condition) throw new IllegalArgumentException(message)

}
