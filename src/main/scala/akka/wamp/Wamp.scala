package akka.wamp

import java.net.InetSocketAddress

import akka.actor.{ActorRef, ExtendedActorSystem, ExtensionId, Props}
import akka.io.Inet.SocketOption
import akka.io.{IO, TcpExt}
import akka.stream.ActorMaterializer
import akka.wamp.serialization.JsonSerialization

import scala.collection.immutable


/**
  * WAMP Extension for Akka’s IO layer.
  *
  * For a full description of the design and philosophy behind this IO
  * implementation please refer to <a href="http://doc.akka.io/">the Akka online documentation</a>.
  *
  * In order to open an outbound connection send a [[Wamp.Connect]] message
  * to the [[WampExt#manager]].
  *
  * In order to start listening for inbound connections send a [[Wamp.Bind]]
  * message to the [[WampExt#manager]].
  */
object Wamp extends ExtensionId[WampExt] with WampExtMessages {
  override def createExtension(system: ExtendedActorSystem): WampExt = 
    new WampExt(system)
}


/*TODO private[wamp]*/ trait WampExtMessages {
  sealed trait Message

  trait Command extends Message
  trait Signal extends Message

  /**
    * The Connect message is sent to the WAMP manager actor, which is obtained via
    * [[WampExt#manager]]. Either the manager replies with a [[CommandFailed]]
    * or the actor handling the new connection replies with a [[Connected]]
    * message.
    *
    * @param uri is the URI to connect to (e.g. "ws://somehost.com:8080/wamp")
    * @param subprotocol is the WebSocket subprotocol (e.g. "wamp.2.json" or "wamp.2.msgpack")
    */
  final case class Connect(uri: String, subprotocol: String) extends Command
  
  /**
    * The Bind message is send to the WAMP manager actor, which is obtained via
    * [[WampExt#manager]] in order to bind to a listening socket. The manager
    * replies either with a [[CommandFailed]] or the actor handling the listen
    * socket replies with a [[Bound]] message. If the local port is set to 0 in
    * the Bind message, then the [[Bound]] message should be inspected to find
    * the actual port which was bound to.
    *
    * @param handler is actor which will receive all incoming connection requests in the form of [[Connected]] messages
    * @param iface   is the socket interface to bind to (use "0.0.0.0" to bind to all of the underlying interfaces)
    * @param port    is the socket port to bind to (use port zero for automatic assignment (i.e. an ephemeral port, see [[Bound]])
    */
  final case class Bind(handler: ActorRef, iface: String, port: Tpe) extends Command

  /**
    * The sender of a [[Bind]] command will — in case of success — receive confirmation
    * in this form. If the bind address indicated a 0 port number, then the contained
    * `localAddress` can be used to find out which port was automatically assigned.
    */
  final case class Bound(localAddress: InetSocketAddress) extends Signal
  
  final case object Unbind
  
  //TODO final case class Bound(???) extends Command
  //TODO final case class Register(???) extends Command

  /**
    * The connection actor sends this message either to the sender of a [[Connect]]
    * command (for outbound) or to the handler for incoming connections designated
    * in the [[Bind]] message. 
    */  
  final case class Connected(ref: ActorRef) extends Signal
  
  final case class CommandFailed(cmd: Command) extends Signal
  
  final case object Disconnected extends Signal

  final case object Disconnect extends Command
  
  final case class Failure(message: String) extends Signal
  
  sealed trait WampMessage extends Message {
    val tpe: Tpe
    // TODO def toJson = ???
  }
  
  case class Payload(val arguments: Either[List[Any], Map[String, Any]])
  object Payload {
    def apply(arguments: List[Any]): Payload = new Payload(Left(arguments))
    def apply(arguments: Map[String, Any]): Payload = new Payload(Right(arguments))
    def isValid(payload: Option[Payload]) = payload match {
      case Some(p) if (p != null) => p.arguments match {
        case Right(args) if (args != null) => true
        case Left(args) if (args != null) => true
        case _ => false
      }
      case None => true
      case _ => false
    }
  }

  private def require(condition: Boolean, message: String) = 
    if (!condition) throw new IllegalArgumentException(message)


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

  /**
    * An identifier generator
    */
  type IdGenerator = (Id) => Id

  final object Id {
    val MIN = 0L
    val MAX = 9007199254740992L
    def draw = (scala.util.Random.nextDouble() * MAX).toLong
    def isValid(id: Id) : Boolean = id >= MIN && id <= MAX 
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
    *  Dictionary for options and details elements
    */
  type Dict = Map[String, Any]

  final object Dict {
    def apply(): Dict = Map.empty[String, Any]
    def apply(entries: (String, Any)*): Dict = entries.toMap
  }

  /* Some WAMP messages contain "Options|dict" or "Details|dict" elements.
  * This allows for future extensibility and implementations that only
  * provide subsets of functionality by ignoring unimplemented
  * attributes.  
  * TODO Keys in "Options" and "Details" MUST be of type "string" and MUST match the regular expression "[a-z][a-z0-9_]{2,}" 
  * for WAMP _predefined_ keys.  Implementations MAY use implementation-specific
  * keys that MUST match the regular expression "_[a-z0-9_]{3,}".
  * Attributes unknown to an implementation MUST be ignored.
  */

  implicit class RichDict(dict: Dict) {
    def withRoles(roles: String*): Dict = {
      dict ++ Map("roles" -> roles.map(role => (role -> Map())).toMap)
    }
    def withAgent(agent: String): Dict = {
      dict + ("agent" -> agent)
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
    *  - implementations only supporting subsets of functionality
    *  - future extensibility
    *
    * A Client must announce the roles it supports via "Hello.Details.roles|dict", 
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
  final case class Hello(realm: Uri, details: Dict) extends WampMessage {
    val tpe = Tpe.HELLO
    require(Uri.isValid(realm), "invalid_uri")
    require(details != null, "invalid_dict")
    require(details.isDefinedAt("roles"), "invalid_roles")
    require(!details.roles.isEmpty, "invalid_roles")
    require(details.roles.forall(isValid), "invalid_roles")
    private def isValid(role: String) = Seq("publisher", "subscriber", "caller", "callee").contains(role)
  }

  
  /**
    * Sent by a Router to accept a Client and let it know the Session is now open
    *
    * ```
    * [WELCOME, Session|id, Details|dict]
    * ```
    *
    * @param sessionId is the session identifier
    * @param details is the session details
    */
  final case class Welcome(sessionId: Id, details: Dict) extends WampMessage {
    val tpe = Tpe.WELCOME
    require(Id.isValid(sessionId), "invalid_id")
    require(details != null, "invalid_dict")
  }

  
  /**
    * Sent by a Peer to abort the opening of a Session.
    * No response is expected.
    *
    * ```
    * [ABORT, Details|dict, Reason|uri]
    * ```
    */
  final case class Abort(details: Dict, reason: Uri) extends WampMessage {
    val tpe = Tpe.ABORT
    require(details != null, "invalid_dict")
    require(Uri.isValid(reason), "invalid_uri")
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
  final case class Goodbye(details: Dict, reason: Uri) extends WampMessage {
    val tpe = Tpe.GOODBYE
    require(details != null, "invalid_dict")
    require(Uri.isValid(reason), "invalid_uri")
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
  final case class Error(requestType: Int, requestId: Id, details: Dict, error: Uri, payload: Option[Payload] = None) extends WampMessage {
    val tpe = Tpe.ERROR
    require(Tpe.isValid(requestType), "invalid_type")
    require(Id.isValid(requestId), "invalid_id")
    require(details != null, "invalid_dict")
    require(Uri.isValid(error), "invalid_uri")
    require(Payload.isValid(payload), "invalid_payload")
    
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
    * @param options is a dictionary that allows to provide additional publication request details in an extensible way.
    * @param topic is the topic published to.
    * @param payload is either a list of any arguments or a key-value-pairs set 
    */
  final case class Publish(requestId: Id, options: Dict, topic: Uri, payload: Option[Payload] = None) extends WampMessage {
    val tpe = Tpe.PUBLISH
    require(Id.isValid(requestId), "invalid_id")
    require(options != null, "invalid_dict")
    require(Uri.isValid(topic), "invalid_uri")
    require(Payload.isValid(payload), "invalid_payload")
  }
  

  /**
    * Acknowledge sent by a Broker to a Publisher for acknowledged Publications.
    *
    * ```
    * [PUBLISHED, PUBLISH.Request|id, Publication|id]
    * ```
    */
  final case class Published(requestId: Id, publicationId: Id) extends WampMessage {
    val tpe = Tpe.PUBLISHED
    require(Id.isValid(requestId), "invalid_id")
    require(Id.isValid(publicationId), "invalid_id")
  }
  

  /**
    * Subscribe request sent by a Subscriber to a Broker to subscribe to a Topic.
    *
    * ```
    * [SUBSCRIBE, Request|id, Options|dict, Topic|uri]
    * ```
    *
    * @param requestId is a random, ephemeral ID chosen by the Subscribe and used to correlate the Broker's response with the request
    * @param options is a dictionary that allows to provide additional subscription request details in a extensible way
    * @param topic is the topic the Subscribe  wants to subscribe to 
    */
  final case class Subscribe(requestId: Id, options: Dict, topic: Uri) extends WampMessage {
    val tpe = Tpe.SUBSCRIBE
    require(Id.isValid(requestId), "invalid_id")
    require(options != null, "invalid_dict")
    require(Uri.isValid(topic), "invalid_uri")
  }



  /**
    * Acknowledge sent by a Broker to a Subscriber to acknowledge a subscription.
    *
    * ```
    * [SUBSCRIBED, SUBSCRIBE.Request|id, Subscription|id]
    * ```
    *
    * @param requestId is the ID from the original Subscribe request
    * @param subscriptionId is an ID chosen by the Broker for the subscription
    */
  final case class Subscribed(requestId: Id, subscriptionId: Id) extends WampMessage {
    val tpe = Tpe.SUBSCRIBED
    require(Id.isValid(requestId), "invalid_id")
    require(Id.isValid(subscriptionId), "invalid_id")
  }
  

  /**
    * Unsubscribe request sent by a Subscriber to a Broker to unsubscribe from a Subscription.
    * ```
    * [UNSUBSCRIBE, Request|id, SUBSCRIBED.Subscription|id]
    * ```
    *
    * @param requestId is a random, ephemeral ID chosen by the Unsubscribe and used to correlate the Broker's response with the request
    * @param subscriptionId is the ID for the subscription to unsubscribe from, originally handed out by the Broker to the Subscriber
    */
  final case class Unsubscribe(requestId: Id, subscriptionId: Id) extends WampMessage {
    val tpe = Tpe.UNSUBSCRIBE
    require(Id.isValid(requestId), "invalid_id")
    require(Id.isValid(subscriptionId), "invalid_id")
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
  final case class Unsubscribed(requestId: Id) extends WampMessage {
    val tpe = Tpe.UNSUBSCRIBED
    require(Id.isValid(requestId), "invalid_id")
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
    * @param publicationId is the ID of the publication of the published event
    * @param details is a dictionary that allows to provide additional event details in an extensible way.
    * @param payload is either a list of any arguments or a key-value-pairs set
    */
  final case class Event(subscriptionId: Id, publicationId: Id, details: Dict, payload: Option[Payload] = None) extends WampMessage {
    val tpe = Tpe.EVENT
    require(Id.isValid(subscriptionId), "invalid_id")
    require(Id.isValid(publicationId), "invalid_id")
    require(details != null, "invalid_dict")
    require(Payload.isValid(payload), "invalid_payload")
  }
}




class WampExt(system: ExtendedActorSystem) extends IO.Extension {
  implicit val s = system
  implicit val m = ActorMaterializer()
  val manager: ActorRef = {
    system.systemActorOf(
      props = Props(new WampManager()(s, m)),
      name = "IO-WAMP")
  }
}
