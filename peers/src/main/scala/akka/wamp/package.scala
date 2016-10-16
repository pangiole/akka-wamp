package akka

import akka.wamp.messages._

import scala.util.Random.{nextDouble => rnd}

/**
  * == akka-wamp 0.11.0 ==
  *
  * [[http://wamp-proto.org WAMP - Web Application Messaging Protocol]] implementation written in Scala with Akka
  */
package object wamp {

  /**
    * A peer could be either a client or a router
    *  - it must implement one role, and
    *  - may implement more roles.
    */
  type Role = String

  // TODO use a Scala enumeration (or sealed trait)
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
    def isValid(typeCode: TypeCode): Boolean = all.contains(typeCode)
  }

  /**
    * Unique identifiers being used to distinguish Sessions, Publications, 
    * Subscriptions, Registrations and Requests
    */
  type Id = Long
  
  type SessionId = Long
  
  type RequestId = Long
  
  type SubscriptionId = Long
  
  type PublicationId = Long
  
  type RegistrationId = Long


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
    def addRoles(roles: Set[Role]): Dict = {
      addRoles(roles.toList: _*)
    }
    def addRoles(roles: Role*): Dict = {
      dict ++ Map("roles" -> roles.sorted.map(r => (r -> Map())).toMap)
    }

    def setAgent(agent: String): Dict = {
      dict + ("agent" -> agent)
    }
    
    def setAck(bool: Boolean = true): Dict = {
      dict + ("acknowledge" -> bool)
    }
  }
}
