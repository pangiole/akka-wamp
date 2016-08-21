package akka

/**
  * == akka-wamp 0.5.1 ==
  *
  * [[http://wamp-proto.org WAMP - Web Application Messaging Protocol]] implementation written in Scala with Akka
  */
package object wamp {

  /**
    * A Peer could be either a [[Client]] or a [[Router]]
    *  - it must implement one [[Role]], and
    *  - may implement more [[Role]]s.
    */
  type Role = String

  object Roles {
    val Subscriber = "subscriber"
    val Publisher = "publisher"
  }
  
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
    val regex = "^([0-9a-z_]+\\.)*([0-9a-z_]+)$".r
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

  implicit class RichDict(dict: Dict) {
    def withRoles(roles: Role*): Dict = {
      dict ++ Map("roles" -> roles.map(role => (role -> Map())).toMap)
    }

    def withAgent(agent: String): Dict = {
      dict + ("agent" -> agent)
    }
    
    def withAcknowledge(bool: Boolean = true): Dict = {
      dict + ("acknowledge" -> bool)
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
        List(Nil, asDict)
      } else {
        List(arguments)
      }
    }
    
    def asList: List[Any] = arguments

    def asDict: Map[String, Any] = {
      arguments.zipWithIndex.map {
        case (arg: Tuple2[_, _], _) => (arg._1.toString, arg._2)
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
