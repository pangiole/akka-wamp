package akka.wamp.messages

import akka.wamp._
import akka.wamp.Wamp._
import akka.wamp.router.Session
import akka.wamp.serialization.Payload

/**
  * Common interface of WAMP messages exchanged by two peers during a [[Session]]
  */
sealed trait Message extends AbstractMessage {
  protected val tpe: TypeCode
}


/**
  * Sent by a Client to initiate opening of a Session to a Router
  * attaching to a Realm.
  *
  * ```
  * [HELLO, Realm|uri, Details|dict]
  * ```
  *
  * WAMP uses "roles & features announcement" instead of "protocol versioning" to allow
  *
  * - implementations only supporting subsets of functionality
  * - future extensibility
  *
  * A Client must announce the roles it supports via "Hello.Details.roles|dict", 
  * with a key mapping to a "Hello.Details.roles.<role>|dict" where "<role>" can be:
  *
  * - "publisher"
  * - "subscriber"
  * - "caller"
  * - "callee"
  *
  * @param realm
  * @param details
  */
final case class Hello(realm: Uri = "akka.wamp.realm", details: Dict = Hello.defaultDetails)(implicit validator: Validator) extends Message {
  protected val tpe = Hello.tpe
  validator.validate(realm)
  require(details != null, s"invalid dict $details")
  require(details.isDefinedAt("roles") && !details.roles.isEmpty, s"missing roles in dict ${details}")
  require(details.roles.forall(isValid), s"invalid roles in dict $details")

  private def isValid(role: String) = Seq("publisher", "subscriber", "caller", "callee").contains(role)
}
final object Hello {
  val tpe = 1
  val defaultDetails = Dict("roles" -> Map("publisher" -> Map(), "subscriber" -> Map()))
}


/**
  * Sent by a Router to accept a Client and let it know the Session is now open
  *
  * ```
  * [WELCOME, Session|id, Details|dict]
  * ```
  *
  * @param sessionId is the session identifier
  * @param details   is the session details
  */
final case class Welcome(sessionId: Id, details: Dict = Welcome.defaultDetails)(implicit validator: Validator) extends Message {
  protected val tpe = Welcome.tpe
  validator.validate(sessionId)
  require(details != null, "invalid Dict")
}
final object Welcome {
  val tpe = 2
  val defaultDetails = Dict()
}

/**
  * Sent by a peer to abort the opening of a Session.
  * No response is expected.
  *
  * @param details is a dictionary (empty by default) that allows to provide additional and optional closing information
  * @param reason is the reason given as URI (e.g. "wamp.error.no_such_realm")
  */
final case class Abort(details: Dict = Abort.defaultDetails, reason: Uri)(implicit validator: Validator) extends Message {
  protected val tpe = Abort.tpe
  require(details != null, "invalid Dict")
  validator.validate(reason)
}
final object Abort {
  val tpe = 3
  val defaultDetails = Dict()
}


/**
  * Sent by a peer to close a previously opened Session.  
  * Must be echo'ed by the receiving peer.
  *
  * ```
  * [GOODBYE, Details|dict, Reason|uri]
  * ```
 *
  * @param details is a dictionary (empty by default) that allows to provide additional and optional closing information
  * @param reason is the reason ("wamp.error.close_realm" by default) given as URI
  */
final case class Goodbye(details: Dict = Goodbye.defaultDetails, reason: Uri = Goodbye.defaultReason)(implicit validator: Validator) extends Message {
  protected val tpe = Goodbye.tpe
  require(details != null, "invalid Dict")
  validator.validate(reason)
}
final object Goodbye {
  val tpe = 6
  val defaultReason = "wamp.error.close_realm"
  val defaultDetails = Dict()
}


/**
  * Error reply sent by a peer as an error response to different kinds of requests.
  *
  * ```
  * [ERROR, REQUEST.Type|int, REQUEST.Request|id, Details|dict, Error|uri
  * [ERROR, REQUEST.Type|int, REQUEST.Request|id, Details|dict, Error|uri, Arguments|list]
  * [ERROR, REQUEST.Type|int, REQUEST.Request|id, Details|dict, Error|uri, Arguments|list, ArgumentsKw|dict]
  * ```
  *
  * @param requestType
  * @param requestId
  * @param error
  * @param details
  * @param payload is either a list of any arguments or a key-value-pairs set
  */
final case class Error(requestType: Int, requestId: Id, details: Dict = Error.defaultDetails, error: Uri, payload: Option[Payload] = None)(implicit validator: Validator) extends Message {
  protected val tpe = Error.tpe
  require(TypeCode.isValid(requestType), "invalid Type")
  validator.validate(requestId)
  require(details != null, "invalid Dict")
  validator.validate(error)
}
final object Error {
  val tpe = 8
  val defaultDetails = Dict()
}


/**
  * Sent by a Publisher to a Broker to publish an Event.
  *
  * ```
  * [PUBLISH, Request|id, Options|dict, Topic|uri]
  * [PUBLISH, Request|id, Options|dict, Topic|uri, Arguments|list]
  * [PUBLISH, Request|id, Options|dict, Topic|uri, Arguments|list, ArgumentsKw|dict]
  * ```
  *
  * @param requestId is a random, ephemeral ID chosen by the Publisher and used to correlate the Broker's response with the request.
  * @param topic     is the topic published to.
  * @param options   is a dictionary that allows to provide additional publication request details in an extensible way.
  * @param payload   is either a list of any arguments or a key-value-pairs set 
  */
final case class Publish(requestId: Id, options: Dict = Publish.defaultOptions, topic: Uri, payload: Option[Payload] = None)(implicit validator: Validator) extends Message {
  protected val tpe = Publish.tpe
  validator.validate(requestId)
  require(options != null, "invalid Dict")
  validator.validate(topic)
}
final object Publish {
  val tpe = 16
  val defaultOptions = Dict()
}

/**
  * Acknowledge sent by a Broker to a Publisher for acknowledged Publications.
  *
  * ```
  * [PUBLISHED, PUBLISH.Request|id, Publication|id]
  * ```
  */
final case class Published(requestId: Id, publicationId: Id)(implicit validator: Validator) extends Message {
  protected val tpe = Published.tpe
  validator.validate(requestId)
  validator.validate(publicationId)
}
final object Published {
  val tpe = 17
  val defaultOptions = Dict()
}


/**
  * Subscribe request sent by a Subscriber to a Broker to subscribe to a Topic.
  *
  * ```
  * [SUBSCRIBE, Request|id, Options|dict, Topic|uri]
  * ```
  *
  * @param requestId is a random, ephemeral ID chosen by the Subscribe and used to correlate the Broker's response with the request
  * @param options   is a dictionary that allows to provide additional subscription request details in a extensible way
  * @param topic     is the topic the Subscribe  wants to subscribe to 
  */
final case class Subscribe(requestId: Id, options: Dict = Subscribe.defaultOptions, topic: Uri)(implicit validator: Validator) extends Message {
  protected val tpe = Subscribe.tpe
  validator.validate(requestId)
  require(options != null, "invalid Dict")
  validator.validate(topic)
}
final object Subscribe {
  val tpe = 32
  val defaultOptions = Dict()
}


/**
  * Acknowledge sent by a Broker to a Subscriber to acknowledge a subscription.
  *
  * ```
  * [SUBSCRIBED, SUBSCRIBE.Request|id, Subscription|id]
  * ```
  *
  * @param requestId      is the ID from the original Subscribe request
  * @param subscriptionId is an ID chosen by the Broker for the subscription
  */
final case class Subscribed(requestId: Id, subscriptionId: Id) (implicit validator: Validator) extends Message {
  protected val tpe = Subscribed.tpe
  validator.validate(requestId)
  validator.validate(subscriptionId)
}
final object Subscribed {
  val tpe = 33
}

/**
  * Unsubscribe request sent by a Subscriber to a Broker to unsubscribe from a Subscription.
  * ```
  * [UNSUBSCRIBE, Request|id, SUBSCRIBED.Subscription|id]
  * ```
  *
  * @param requestId      is a random, ephemeral ID chosen by the Unsubscribe and used to correlate the Broker's response with the request
  * @param subscriptionId is the ID for the subscription to unsubscribe from, originally handed out by the Broker to the Subscriber
  */
final case class Unsubscribe(requestId: Id, subscriptionId: Id) (implicit validator: Validator) extends Message {
  protected val tpe = Unsubscribe.tpe
  validator.validate(requestId)
  validator.validate(subscriptionId)
}
final object Unsubscribe {
  val tpe = 34
}


/**
  *
  * Acknowledge sent by a Broker to a Subscriber to acknowledge unsubscription.
  *
  * ```
  * [UNSUBSCRIBED, UNSUBSCRIBE.Request|id]
  * ```
  *
  * @param requestId is the ID from the original Subscribed request
  */
final case class Unsubscribed(requestId: Id) (implicit validator: Validator) extends Message {
  protected val tpe = Unsubscribed.tpe
  validator.validate(requestId)
}
final object Unsubscribed {
  val tpe = 35
}

/**
  * Event dispatched by Broker to Subscribers for Subscriptions the event was matching.
  *
  * ```
  * [EVENT, SUBSCRIBED.Subscription|id, PUBLISHED.Publication|id, Details|dict]
  * [EVENT, SUBSCRIBED.Subscription|id, PUBLISHED.Publication|id, Details|dict, Arguments|list]
  * [EVENT, SUBSCRIBED.Subscription|id, PUBLISHED.Publication|id, Details|dict, Arguments|list, ArgumentsKw|dict]
  * ```
  *
  * @param subscriptionId is the ID for the subscription under which the Subscribe receives the event (the ID for the subscription originally handed out by the Broker to the Subscriber.
  * @param publicationId  is the ID of the publication of the published event
  * @param payload        is either a list of any arguments or a key-value-pairs set
  * @param details        is a dictionary that allows to provide additional event details in an extensible way.
  */
final case class Event(subscriptionId: Id, publicationId: Id, details: Dict = Event.defaultOptions, payload: Option[Payload] = None)(implicit validator: Validator) extends Message {
  protected val tpe = Event.tpe
  validator.validate(subscriptionId)
  validator.validate(publicationId)
  require(details != null, "invalid Dict")
}
final object Event {
  val tpe = 36
  val defaultOptions = Dict()
}
