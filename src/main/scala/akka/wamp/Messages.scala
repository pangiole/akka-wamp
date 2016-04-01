package akka.wamp


object Messages {
  
  val HELLO = 1
  val WELCOME = 2
  val ABORT = 3
  val GOODBYE = 6
  val ERROR = 8
  val SUBSCRIBE = 32
  val SUBSCRIBED = 33
  val UNSUBSCRIBE = 34
  val UNSUBSCRIBED = 35
  
  /**
    * Build a message instance
    */
  trait Builder {
    def fail(message: String) = throw new IllegalArgumentException(message)
    def require(condition: Boolean, message: String) = if (!condition) fail(message)
    def build(): Message
  }



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




  /**
    * Sent by a [[Router]] to accept a [[Client]] to let it know the [[Session]] is now open
    *
    * ```
    * [WELCOME, Session|id, Details|dict]
    * ```
    *
    *
    * @param sessionId
    * @param details
    */
  case class Welcome(sessionId: Long, details: Dict) extends Message(WELCOME)

  

  /**
    * Sent by a [[Peer]] to close a previously opened [[Session]].  
    * Must be echo'ed by the receiving Peer.
    *
    * ```
    * [GOODBYE, Details|dict, Reason|uri]
    * ```
    *
    * @param details
    * @param reason
    */
  case class Goodbye(details: Dict, reason: Uri) extends Message(GOODBYE)


  /**
    * Build an [[Goodbye]] instance.
    */
  class GoodbyeBuilder() extends Builder {
    var details: Dict = _
    var reason: Uri = _

    def build(): Message = {
      require(details != null, "missing details dict")
      require(reason != null, "missing reason uri")
      new Goodbye(details, reason)
    }
  }
  

  /**
    * Sent by a [[Peer]] to abort the opening of a [[Session]].
    * No response is expected.
    *
    * ```
    * [ABORT, Details|dict, Reason|uri]
    * ```
    */
  case class Abort(details: Dict, reason: Uri) extends Message(ABORT)




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
    */
  case class Error(requestType: Int, requestId: Long, details: Dict, error: Uri) extends Message(ERROR)



  /**
    * Subscribe request sent by a [[Subscriber]] to a [[Broker]] to subscribe to a [[Topic]].
    *
    * ```
    * [SUBSCRIBE, Request|id, Options|dict, Topic|uri]
    * ```
    *
    * @param requestId is a random, ephemeral ID chosen by the [[Subscribe]] and used to correlate the [[Broker]]'s response with the request
    * @param options is a dictionary that allows to provide additional subscription request details in a extensible way
    * @param topic is the topic the [[Subscribe]]  wants to subscribe to 
    */
  case class Subscribe(requestId: Id, options: Dict, topic: Uri) extends Message(SUBSCRIBE)



  /**
    * Build an [[Subscribe]] instance.
    */
  class SubscribeBuilder() extends Builder {
    var requestId: Id = -1
    var options: Dict = _
    var topic: Uri = _
    override def build(): Message = {
      require(requestId != -1, "missing requestId")
      require(options != null, "missing options dict")
      require(topic != null, "missing topic uri")
      new Subscribe(requestId, options, topic)
    }
  }



  /**
    * Acknowledge sent by a [[Broker]] to a [[Subscriber]] to acknowledge a subscription.
    *
    * ```
    * [SUBSCRIBED, SUBSCRIBE.Request|id, Subscription|id]
    * ```
    *
    * @param requestId is the ID from the original [[Subscribe]] request
    * @param subscriptionId is an ID chosen by the [[Broker]] for the subscription
    */
  case class Subscribed(requestId: Id, subscriptionId: Id) extends Message(SUBSCRIBED)
  

  /**
    * Unsubscribe request sent by a [[Subscriber]] to a [[Broker]] to unsubscribe from a [[Subscription]].
    * ```
    * [UNSUBSCRIBE, Request|id, SUBSCRIBED.Subscription|id]
    * ```
    *
    * @param requestId is a random, ephemeral ID chosen by the [[Unsubscribe]] and used to correlate the [[Broker]]'s response with the request
    * @param subscriptionId is the ID for the subscription to unsubscribe from, originally handed out by the [[Broker]] to the [[Subscriber]]
    */
  case class Unsubscribe(requestId: Id, subscriptionId: Id) extends Message(UNSUBSCRIBE)



  /**
    * Build an [[Unsubscribe]] instance.
    */
  class UnsubscribeBuilder() extends Builder {
    var requestId: Id = -1
    var subscriptionId: Id = -1

    override def build(): Message = {
      require(requestId != -1, "missing requestId")
      require(subscriptionId != -1, "missing subscriptionId")
      new Unsubscribe(requestId, subscriptionId)
    }
  }



  /**
    *
    * Acknowledge sent by a [[akka.wamp.Broker]] to a [[akka.wamp.Subscriber]] to acknowledge unsubscription.
    *
    * ```
    * [UNSUBSCRIBED, UNSUBSCRIBE.Request|id]
    * ```
    *
    * @param requestId is the ID from the original [[Subscribed]] request
    */
  case class Unsubscribed(requestId: Id) extends Message(UNSUBSCRIBED)
}
