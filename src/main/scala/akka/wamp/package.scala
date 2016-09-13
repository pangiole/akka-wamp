package akka

import akka.wamp.messages._

import scala.collection.SortedSet
import scala.util.Random.{nextDouble => rnd}

/**
  * == akka-wamp 0.7.0 ==
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
    val subscriber = "subscriber"
    val publisher = "publisher"
    val callee = "callee"
    val caller = "caller"
    val broker = "broker"
    /**
      * All roles for a fully capable client
      */
    val client = Set(callee, caller, publisher, subscriber)
    val dealer = "dealer"
    /**
      * All roles for a fully capable broker
      */
    val router = Set(broker, dealer)
  }
  
  /**
    * Specified message type
    */
  type TypeCode = Int
  
  final object TypeCode {
    val all = List(
      Hello.tpe, 
      Welcome.tpe, 
      Abort.tpe, 
      Goodbye.tpe,
      Error.tpe,
      Publish.tpe, Published.tpe,
      Subscribe.tpe, Subscribed.tpe,
      Unsubscribe.tpe, Unsubscribed.tpe,
      Event.tpe
    )
    def isValid(typeCode: TypeCode): Boolean = all.contains(typeCode)
  }

  /**
    * Unique identifiers being used to distinguish Sessions, Publications, 
    * Subscriptions, Registrations and Requests
    */
  type Id = Long


  final object Id {
    val min = 1L
    val max = 9007199254740992L
    def draw(): Long = min + (rnd() * (max - min + 1)).toLong
  }

  
  /**
    * Uniform Resource Identifier
    */
  type Uri = String
  
  final object Uri

  /**
    * Dictionary for options and details elements
    */
  type Dict = Map[String, Any]

  final object Dict {
    def apply(): Dict = Map.empty[String, Any]
    def apply(entries: (String, Any)*): Dict = entries.toMap
  }

  implicit class RichDict(dict: Dict) {
    def withRoles(roles: Set[Role]): Dict = {
      withRoles(roles.toList: _*)
    }
    def withRoles(roles: Role*): Dict = {
      dict ++ Map("roles" -> roles.sorted.map(r => (r -> Map())).toMap)
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
  
}
