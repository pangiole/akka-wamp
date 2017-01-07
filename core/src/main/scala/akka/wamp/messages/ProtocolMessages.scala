package akka.wamp.messages

import akka.wamp._
import akka.wamp.serialization._

import scala.concurrent._

/**
  * Represents protocol messages exchanged by [[Peer]]s
  */
sealed trait ProtocolMessage extends Message {

  protected val tpe: TypeCode
}

/**
  * Sent by a client to initiate opening of a session to a router
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
  * A client must announce the roles it supports via "Hello.Details.roles|dict",
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
final case class Hello(
  realm: Uri = "default",
  details: Dict = Hello.defaultDetails)
  (implicit validator: Validator)
  extends ProtocolMessage
{
  val tpe = Hello.tpe
  validator.validate(realm)
  validator.validate(details)
  validator.validateClientRoles(details)
}
final object Hello {
  val tpe = 1
  val defaultRealm = "default"
  val defaultDetails = Dict("roles" -> Roles.client.toList.sorted.map(_ -> Map()).toMap)
}


/**
  * Sent by a router to accept a client and let it know the session is now open
  *
  * ```
  * [WELCOME, Session|id, Details|dict]
  * ```
  *
  * @param sessionId is the session identifier
  * @param details is the session details
  */
final case class Welcome(
  sessionId: Id,
  details: Dict = Welcome.defaultDetails)
  (implicit validator: Validator)
  extends ProtocolMessage
{
  val tpe = Welcome.tpe
  validator.validate(sessionId)
  validator.validate(details)
}
final object Welcome {
  val tpe = 2
  val defaultDetails = Dict()
}

/**
  * Sent by a peer to abort the opening of a session.
  * No response is expected.
  *
  * @param details is a dictionary (empty by default) that allows to provide additional and optional closing information
  * @param reason is the reason given as URI (e.g. "wamp.error.no_such_realm")
  */
final case class Abort(
  details: Dict = Abort.defaultDetails,
  reason: Uri)
  (implicit validator: Validator)
  extends ProtocolMessage
{
  val tpe = Abort.tpe
  validator.validate(details)
  validator.validate(reason)
}
final object Abort {
  val tpe = 3
  val defaultDetails = Dict()
}


/**
  * Sent by a peer to close a previously opened session.
  * Must be echo'ed by the receiving peer.
  *
  * ```
  * [GOODBYE, Details|dict, Reason|uri]
  * ```
 *
  * @param details is a dictionary (empty by default) that allows to  provide additional and optional closing information
  * @param reason is the reason ("wamp.error.close_realm" by default) given as URI
  */
final case class Goodbye(
  details: Dict = Goodbye.defaultDetails,
  reason: Uri = Goodbye.defaultReason)
  (implicit validator: Validator)
  extends ProtocolMessage
{
  val tpe = Goodbye.tpe
  validator.validate(details)
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
  * @param payload is the payload bearing application data
  */
final case class Error(
  requestType: Int,
  requestId: Id,
  details: Dict = Error.defaultDict,
  error: Uri,
  payload: Payload = Payload())
  (implicit validator: Validator)
  extends ProtocolMessage
  with DataConveyor {
  val tpe = Error.tpe
  require(TypeCodes.isValid(requestType), "invalid Type")
  validator.validate(requestId)
  validator.validate(details)
  validator.validate(error)
}
final object Error {
  val tpe = 8
  val defaultDict = Dict()
}




// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//
//    P U B L I S H   a n d   S U B S C R I B E
//
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

/**
  * Sent by a publisher to a broker to publish an event.
  *
  * ```
  * [PUBLISH, Request|id, Options|dict, Topic|uri]
  * [PUBLISH, Request|id, Options|dict, Topic|uri, Arguments|list]
  * [PUBLISH, Request|id, Options|dict, Topic|uri, Arguments|list, ArgumentsKw|dict]
  * ```
  *
  * @param requestId is a random, ephemeral identifier chosen by the Publisher and used  to correlate the Broker's response with the request.
  * @param topic is the topic published to.
  * @param options is a dictionary that allows to provide additional publication  request details in an extensible way.
  * @param payload  is the payload bearing application data
  */
final case class Publish(
  requestId: Id,
  options: Dict = Publish.defaultOptions,
  topic: Uri,
  payload: Payload = Payload())
  (implicit validator: Validator)
  extends ProtocolMessage
  with DataConveyor
{
  val tpe = Publish.tpe
  validator.validate(requestId)
  validator.validate(options)
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
 *
  * @param requestId is the identifier from the original publication request
  * @param publicationId is a identifier chosen by the router for the publication
  */
final case class Published(
  requestId: Id,
  publicationId: Id)
  (implicit validator: Validator)
  extends ProtocolMessage
{
  val tpe = Published.tpe
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
  * @param requestId is a random, ephemeral identifier chosen by the Subscriber and used to  correlate the Broker's response with the request
  * @param options is a dictionary that allows to provide additional subscription  request details in a extensible way
  * @param topic is the topic the Subscribe  wants to subscribe to
  */
final case class Subscribe(
  requestId: Id,
  options: Dict = Subscribe.defaultOptions,
  topic: Uri)
  (implicit validator: Validator)
  extends ProtocolMessage
{
  val tpe = Subscribe.tpe
  validator.validate(requestId)
  validator.validate(options)
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
  * @param requestId is the identifier from the original Subscribe request
  * @param subscriptionId is an identifier chosen by the Broker for the subscription
  */
final case class Subscribed(
  requestId: Id,
  subscriptionId: Id)
  (implicit validator: Validator)
  extends ProtocolMessage
{
  val tpe = Subscribed.tpe
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
  * @param requestId  is a random, ephemeral identifier chosen by the Unsubscribe and used to correlate the Broker's response with the request
  * @param subscriptionId is the identifier for the subscription to unsubscribe from, originally handed out by the Broker to the Subscriber
  */
final case class Unsubscribe(
  requestId: Id,
  subscriptionId: Id)
  (implicit validator: Validator)
  extends ProtocolMessage
{
  val tpe = Unsubscribe.tpe
  validator.validate(requestId)
  validator.validate(subscriptionId)
}
final object Unsubscribe {
  val tpe = 34
}


/**
  *
  * Is the unsubscribed acknowledge
  *
  * ```
  * [UNSUBSCRIBED, UNSUBSCRIBE.Request|id]
  * ```
  *
  * @param requestId is the identifier from the original Subscribed request
  */
final case class Unsubscribed(
  requestId: Id)
  (implicit validator: Validator)
  extends ProtocolMessage
{
  val tpe = Unsubscribed.tpe
  validator.validate(requestId)
}
final object Unsubscribed {
  val tpe = 35
}

/**
  * Event dispatched by brokers to subscribers for subscriptions the event was matching.
  *
  * ```
  * [EVENT, SUBSCRIBED.Subscription|id, PUBLISHED.Publication|id, Details|dict]
  * [EVENT, SUBSCRIBED.Subscription|id, PUBLISHED.Publication|id, Details|dict, Arguments|list]
  * [EVENT, SUBSCRIBED.Subscription|id, PUBLISHED.Publication|id, Details|dict, Arguments|list, ArgumentsKw|dict]
  * ```
  *
  * @param subscriptionId is the identifier of the subscription under which the subscriber receives the event (the identifier for the subscription originally handed out by the broker to the subscriber)
  * @param publicationId  is the identifier of the publication of the published event
  * @param details is a dictionary that allows to provide additional event details in an extensible way
  * @param payload is the payload bearing application data
  */
final case class Event(
  subscriptionId: Id,
  publicationId: Id,
  details: Dict = Event.defaultOptions,
  payload: Payload = Payload())
  (implicit validator: Validator, executionContent: ExecutionContext)
  extends ProtocolMessage
  with DataConveyor
{
  val tpe = Event.tpe
  validator.validate(subscriptionId)
  validator.validate(publicationId)
  validator.validate(details)
}
final object Event {
  val tpe = 36
  val defaultOptions = Dict()
}




// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//
//    R E M O T E   P R O C E D U R E   C A L L s
//
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


/**
  * Register request sent by a callee to a dealer to register a procedure endpoint
  *
  * ```
  * [REGISTER, Request|id, Options|dict, Procedure|uri]
  * ```
  *
  * @param requestId is a random, ephemeral identifier chosen by the Callee and used to correlate the Dealer's response with the request
  * @param options   is a dictionary that allows to provide additional registration request details in a extensible way
  * @param procedure is the procedure the Callee wants to register
  */
final case class Register(
  requestId: Id,
  options: Dict = Register.defaultOptions,
  procedure: Uri)
  (implicit validator: Validator)
  extends ProtocolMessage
{
  val tpe = Register.tpe
  validator.validate(requestId)
  validator.validate(options)
  validator.validate(procedure)
}
final object Register {
  val tpe = 64
  val defaultOptions = Dict()
}


/**
  * Acknowledge sent by a dealer to a callee to acknowledge a registration
  *
  * ```
  * [REGISTERED, REGISTER.Request|id, Registration|id]
  * ```
  *
  * @param requestId is the identifier from the original register request
  * @param registrationId is an identifier chosen by the dealer for the registration
  */
final case class Registered(
  requestId: Id,
  registrationId: Id)
  (implicit validator: Validator)
  extends ProtocolMessage
{
  val tpe = Registered.tpe
  validator.validate(requestId)
  validator.validate(registrationId)
}
final object Registered {
  val tpe = 65
}



/**
  * Unregister request sent by a callee to a dealer to unregister a procedure endpoint.
  *
  * ```
  * [UNREGISTER, Request|id, REGISTERED.Registration|id]
  * ```
  *
  * @param requestId is a random, ephemeral identifier chosen by the callee and used to correlate the dealer's response with the request.
  * @param registrationId is the identifier for the registration to revoke, originally handed out by the dealer to the callee.
  */
final case class Unregister(
  requestId: Id,
  registrationId: Id)
  (implicit validator: Validator)
  extends ProtocolMessage
{
  val tpe = Unregister.tpe
  validator.validate(requestId)
  validator.validate(registrationId)
}
final object Unregister {
  val tpe = 66
}


/**
  *
  * Acknowledge sent by a dealer to a callee to acknowledge unregistration.
  *
  * ```
  * [UNREGISTERED, UNREGISTER.Request|id]
  * ```
  *
  * @param requestId is the identifier from the original Subscribed request
  */
final case class Unregistered(
  requestId: Id)
  (implicit validator: Validator)
  extends ProtocolMessage
{
  val tpe = Unregistered.tpe
  validator.validate(requestId)
}
final object Unregistered {
  val tpe = 67
}



/**
  * Request sent by a caller to a dealer to call a procedure
  *
  * ```
  * [CALL, Request|id, Options|dict, Procedure|uri]
  * [CALL, Request|id, Options|dict, Procedure|uri, Arguments|list]
  * [CALL, Request|id, Options|dict, Procedure|uri, Arguments|list, ArgumentsKw|dict]
  * ```
  *
  * @param requestId is a random, ephemeral identifier chosen by the caller and used to correlate the dealer's response with the request
  * @param options is a dictionary that allows to provide additional call request options in an extensible way
  * @param procedure is the URI of the procedure to be called
  * @param payload is the payload bearing application data
  */
final case class Call(
  requestId: Id,
  options: Dict = Call.defaultOptions,
  procedure: Uri,
  payload: Payload = Payload())
  (implicit validator: Validator)
  extends ProtocolMessage
  with DataConveyor
{
  val tpe = Call.tpe
  validator.validate(requestId)
  validator.validate(options)
  validator.validate(procedure)
}
final object Call {
  val tpe = 48
  val defaultOptions = Dict()
}



/**
  * Invocation dispatched by the dealer to the callee providing the registration the invocation was matching.
  *
  * ```
  * [INVOCATION, Request|id, REGISTERED.Registration|id, Details|dict]
  * [INVOCATION, Request|id, REGISTERED.Registration|id, Details|dict, CALL.Arguments|list]
  * [INVOCATION, Request|id, REGISTERED.Registration|id, Details|dict, CALL.Arguments|list, CALL.ArgumentsKw|dict]
  * ```
  *
  * @param requestId is a random, ephemeral identifier chosen by the dealer and used to correlate the callee's response with the request.
  * @param registrationId is the registration identifier under which the procedure was registered at the dealer.
  * @param details is a dictionary that allows to provide additional invocation request details in an extensible way.
  * @param payload is the payload bearing application data
  */
final case class Invocation (
  requestId: Id,
  registrationId: Id,
  details: Dict = Invocation.defaultDetails,
  payload: Payload = Payload())
  (implicit validator: Validator, executionContext: ExecutionContext)
  extends ProtocolMessage
  with DataConveyor
{
  val tpe = Invocation.tpe
  validator.validate(requestId)
  validator.validate(registrationId)
  validator.validate(details)
}
final object Invocation {
  val tpe = 68
  val defaultDetails = Dict()
}


/**
  * Is the ``YIELD`` message sent by a callee's registered invocation handler to the dealer
  *
  * {{{
  * [YIELD, INVOCATION.Request|id, Options|dict]
  * [YIELD, INVOCATION.Request|id, Options|dict, Arguments|list]
  * [YIELD, INVOCATION.Request|id, Options|dict, Arguments|list, ArgumentsKw|dict]
  * }}}
  *
  * @param requestId is the identifier from the original invocation request
  * @param options is a dictionary that allows to provide additional options in an extensible way
  * @param payload is the payload conveyed by this message
  */
final case class Yield(
  requestId: Id,
  options: Dict = Yield.defaultOptions,
  payload: Payload = Payload())
  (implicit validator: Validator)
  extends ProtocolMessage
  with DataConveyor
{
  val tpe = Yield.tpe
  validator.validate(requestId)
  validator.validate(options)
}

/**
  * Defines some useful constants
  */
final object Yield {
  /** Is the type code of this message */
  val tpe = 70
  /** Is the default options for this kind of message */
  val defaultOptions = Dict()
}


/**
  * Result of a call as returned by dealer to caller
  *
  * ```
  * [RESULT, CALL.Request|id, Details|dict]
  * [RESULT, CALL.Request|id, Details|dict, Arguments|list]
  * [RESULT, CALL.Request|id, Details|dict, Arguments|list, ArgumentsKw|dict]
  * ```
  *
  * @param requestId is the identifiers from the original call request
  * @param details is a dictionary that allows to provide additional details in an extensible way
  * @param payload is the payload bearing application data
  */
final case class Result(
  requestId: Id,
  details: Dict = Result.defaultDetails,
  payload: Payload = Payload())
  (implicit validator: Validator, ec: ExecutionContext)
  extends ProtocolMessage
  with DataConveyor
{
  val tpe = Result.tpe
  validator.validate(requestId)
  validator.validate(details)
}
final object Result {
  val tpe = 50
  val defaultDetails = Dict()
}