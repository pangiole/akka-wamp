package akka.wamp.messages

import akka.wamp._
import akka.wamp.Wamp._
import akka.wamp.router.Session
import com.typesafe.config.Config

/**
  * Common interface of WAMP messages exchanged by two [[Peer]]s during a [[Session]]
  */
sealed trait Message extends AbstractMessage {
  protected val tpe: Tpe
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
final case class Hello(realm: Uri = "akka.wamp.realm", details: Dict = Hello.DefaultDetails)(implicit validator: Validator) extends Message {
  protected val tpe = Tpe.HELLO
  validator.validate(realm)
  require(details != null, s"invalid dict $details")
  require(details.isDefinedAt("roles") && !details.roles.isEmpty, s"missing roles in dict ${details}")
  require(details.roles.forall(isValid), s"invalid roles in dict $details")

  private def isValid(role: String) = Seq("publisher", "subscriber", "caller", "callee").contains(role)
}
final object Hello {
  val DefaultDetails = Dict("roles" -> Map("publisher" -> Map(), "subscriber" -> Map()))
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
final case class Welcome(sessionId: Id, details: Dict = Welcome.DefaultDetails)(implicit validator: Validator) extends Message {
  protected val tpe = Tpe.WELCOME
  validator.validate(sessionId)
  require(details != null, "invalid Dict")
}
final object Welcome {
  val DefaultDetails = Dict()
}

/**
  * Sent by a Peer to abort the opening of a Session.
  * No response is expected.
  *
  * ```
  * [ABORT, Details|dict, Reason|uri]
  * ```
  */
final case class Abort(reason: Uri, details: Dict = Abort.DefaultDetails)(implicit validator: Validator) extends Message {
  protected val tpe = Tpe.ABORT
  validator.validate(reason)
  require(details != null, "invalid Dict")
}
final object Abort {
  val DefaultDetails = Dict()
}


/**
  * Sent by a Peer to close a previously opened Session.  
  * Must be echo'ed by the receiving Peer.
  *
  * ```
  * [GOODBYE, Details|dict, Reason|uri]
  * ```
  *
  * @param details
  * @param reason
  */
final case class Goodbye(reason: Uri = Goodbye.DefaultReason, details: Dict = Goodbye.DefaultDetails)(implicit validator: Validator) extends Message {
  protected val tpe = Tpe.GOODBYE
  validator.validate(reason)
  require(details != null, "invalid Dict")
}
final object Goodbye {
  val DefaultReason = "wamp.error.close_realm"
  val DefaultDetails = Dict()
}


/**
  * Error reply sent by a Peer as an error response to different kinds of requests.
  *
  * ```
  * [ERROR, REQUEST.Type|int, REQUEST.Request|id, Details|dict, Error|uri
  * [ERROR, REQUEST.Type|int, REQUEST.Request|id, Details|dict, Error|uri, Arguments|list]
  * [ERROR, REQUEST.Type|int, REQUEST.Request|id, Details|dict, Error|uri, Arguments|list, ArgumentsKw|dict]
  * ```
  *
  * @param requestType
  * @param requestId
  * @param details
  * @param error
  * @param payload is either a list of any arguments or a key-value-pairs set
  */
final case class Error(requestType: Int, requestId: Id, details: Dict, error: Uri, payload: Option[Payload] = None)(implicit validator: Validator) extends Message {
  protected val tpe = Tpe.ERROR
  require(Tpe.isValid(requestType), "invalid_type")
  validator.validate(requestId)
  require(details != null, "invalid Dict")
  validator.validate(error)
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
  * @param payload   is either a list of any arguments or a key-value-pairs set 
  * @param options   is a dictionary that allows to provide additional publication request details in an extensible way.
  */
final case class Publish(requestId: Id, topic: Uri, payload: Option[Payload] = None, options: Dict = Dict())(implicit validator: Validator) extends Message {
  protected val tpe = Tpe.PUBLISH
  validator.validate(requestId)
  validator.validate(topic)
  require(options != null, "invalid Dict")
}


/**
  * Acknowledge sent by a Broker to a Publisher for acknowledged Publications.
  *
  * ```
  * [PUBLISHED, PUBLISH.Request|id, Publication|id]
  * ```
  */
final case class Published(requestId: Id, publicationId: Id)(implicit validator: Validator) extends Message {
  protected val tpe = Tpe.PUBLISHED
  validator.validate(requestId)
  validator.validate(publicationId)
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
final case class Subscribe(requestId: Id, topic: Uri, options: Dict = Subscribe.DefaultOptions)(implicit validator: Validator) extends Message {
  protected val tpe = Tpe.SUBSCRIBE
  validator.validate(requestId)
  validator.validate(topic)
  require(options != null, "invalid Dict")
}
final object Subscribe {
  val DefaultOptions = Dict()
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
  protected val tpe = Tpe.SUBSCRIBED
  validator.validate(requestId)
  validator.validate(subscriptionId)
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
  protected val tpe = Tpe.UNSUBSCRIBE
  validator.validate(requestId)
  validator.validate(subscriptionId)
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
  protected val tpe = Tpe.UNSUBSCRIBED
  validator.validate(requestId)
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
  * @param details        is a dictionary that allows to provide additional event details in an extensible way.
  * @param payload        is either a list of any arguments or a key-value-pairs set
  */
final case class Event(subscriptionId: Id, publicationId: Id, details: Dict, payload: Option[Payload] = None) (implicit validator: Validator) extends Message {
  protected val tpe = Tpe.EVENT
  validator.validate(subscriptionId)
  validator.validate(publicationId)
  require(details != null, "invalid Dict")
}
