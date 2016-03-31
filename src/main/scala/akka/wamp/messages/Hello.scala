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
  var realm: Uri = _ 
  var details: Dict = _
  
  def build() = {
    require(realm != null, "missing realm uri")
    require(details != null, "missing details dict")
    require(details.isDefinedAt("roles"), "missing details.roles dict")
    details("roles") match {
      case roles: Map[String, _] =>
        require(!roles.isEmpty, "empty details.roles dict")
        require(roles.keySet.forall(ValidRoles.contains(_)), "invalid details.roles dict")    
      case _ => fail("invalid details.roles dict") 
    }
    new Hello(realm, details)
  }
  val ValidRoles = Seq("publisher", "subscriber", "caller", "callee")
}

