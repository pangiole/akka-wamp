package akka.wamp.messages

import akka.wamp._


/**
  * Sent by a [[Client]] to initiate opening of a [[Session]] to a [[Router]] 
  * attaching to a [[Realm]].
  *
  * ```
  * [HELLO, Realm|uri, Details|dict]
  * ```
  *
  * @param realm
  * @param details
  */
case class Hello(realm: Uri, details: Dict) extends Message(HELLO)


/**
  * Build an [[Hello]] instance.
  *
  * WAMP uses "roles & features announcement" instead of "protocol versioning" to allow
  *
  *  - implementations only supporting subsets of functionality
  *  - future extensibility
  *
  * A [[Client]] must announce the roles it supports via "Hello.Details.roles|dict", 
  * with a key mapping to a "Hello.Details.roles.<role>|dict" where "<role>" can be:
  *
  *  - "publisher"
  *  - "subscriber"
  *  - "caller"
  *  - "callee"
  */
class HelloBuilder  extends Builder {
  var realm: String = _ 
  var details: Dict = _
  
  def build() = {
    check(realm != null, "missing realm")
    check(details != null, "missing details")
    check(details.isDefinedAt("roles"), "missing details.roles")
    details("roles") match {
      case roles: Map[String, _] =>
        check(!roles.isEmpty, "empty details.roles")
        check(roles.keySet.forall(ClientRoles.contains(_)), "unknown details.roles")    
      case _ => fail("invalid details.roles") 
    }
    Hello(realm, details)
  }
  val ClientRoles = Seq("publisher", "subscriber", "caller", "callee")
}

