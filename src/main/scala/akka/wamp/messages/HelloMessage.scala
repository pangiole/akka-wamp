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
case class HelloMessage(realm: Uri, details: Dict) extends Message(1)


/**
  * Build an [[HelloMessage]] instance.
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
  *
  * @param realm
  * @param details
  */
class HelloMessageBuilder private(var realm: String, var details: Dict) extends MessageBuilder {
  def this() = this(null, null)
  def build() = {
    require(realm != null, "Missing realm")
    require(details != null, "Missing details")
    require(details.contains("roles"), "Missing details.roles")
    require(!details("roles").isEmpty, "Empty details.roles")
    HelloMessage(realm, details)
  }
}