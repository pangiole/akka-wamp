# Actors

Akka Wamp provides you with

 * an object-oriented representations of WAMP [messages](./messages.html),
 * an [Akka I/O](http://doc.akka.io/docs/akka/current/scala/io.html) extension driver named ``akka.wamp.Wamp`` that takes care of low-level network communication with a WAMP router,
 * some abstract base classes your client actor can extend from.

All operations are implemented with __message passing__ instead of direct method calls. You can write your own client actor, in either Scala or Java, to connect to a router, open a session, publish or subscribe to topics, consume events, process invocations and register or call procedures.

@@@ note
This shall be considered a __lower level API__ when compared to other Akka Wamp Client APIs such as those providing [Futures](./futures.html) and [Streams](./streams.html) abstractions.
@@@

## Client Actor

Scala
:    @@snip [ScalaClientActor.scala](../../scala/docs/ScalaClientActor.scala){ #client }

Java
:    @@snip [JavaClientActor.java](../../java/docs/JavaClientActor.java){ #client }


## Initial state
You client actor starts in ``initial`` state as _"disconnected from router"_.

### Connect to a router
While in ``initial`` state, your client actor sends a [Connect](../messages.html#Connect) command to the Akka Wamp [extension manager](./index.html#extension-manager) so to attempt a new connection establishment to the router addressed by the given URL. 

Scala
:    @@snip [ScalaClientActor.scala](../../scala/docs/ScalaClientActor.scala){ #connect }

Java
:    @@snip [JavaClientActor.java](../../java/docs/JavaClientActor.java){ #connect }

Your client receives either the [CommandFailed](../messages.html#CommandFailed) signal from the manager if the connection attempt fails or the [Connected](../messages.html#Connected) signal if it succeeds. In the latter case the manager spawns a new worker actor representing the handler for that specific connection and delivers its reference to your client via the signal message.

Upon receiving the [Connected](../messages.html#Connected) signal message, your client can store the connection handler reference and switch its state to ``connected`` 



## Connected state
Your client actor is now _"connected to router"_.

<a name="disconnection"></a>

### Unexpected disconnections
Your connected client actor might receive the [Disconnected](../messages.html#Disconnected) signal from manager if transport disconnects. For example, clients on mobile devices could experience transport disconnections quite often because of wireless networks unavailability.

If that happens, you client actor might keep attempting to establish a new connection, then open a new session and finally restore its subscriptions/registrations ... 

 
@@@ warning
See https://github.com/angiolep/akka-wamp/issues/44
@@@ 

### Open a session
Your client actor sends an [Hello](../messages.html#Hello) message to the router so to attempt a new session opening to the realm identified by the given URI.
 
Scala
:    @@snip [opening-scala](../../scala/docs/ScalaClientActor.scala){ #open }

Java
:    @@snip [opening-java](../../java/docs/JavaClientActor.java){ #open }


Your client actor receives either the [Abort](../messages.html#Abort) message from router if the session opening fails or the [Welcome](../messages.html#Welcome) message if it succeeds. In the latter case the router creates a new session and delivers its identifier to your client via the welcome message.

Upon receiving the [Welcome](../messages.html#Welcome) message, your client can switch to the ``open`` state and then publish to a topic, subscribe to a topic, process events, register a procedure, process invocation, call a procedure, etc.


## Open state
You client actor now _"holds an open session"_.
 
### Unexpected close
In addition to [unexpected disconnections](#disconnection), your client actor might receive the [Goodbye](../messages.html#Goodbye) message from router if session gets closed for some reason. 

If that happens, you client actor might keep attempting to open a new session so to restore its subscriptions/registrations ... 


### Publish to a topic
Scala
:    @@snip [opening-scala](../../scala/docs/ScalaClientActor.scala){ #publish }

Java
:    @@snip [opening-java](../../java/docs/JavaClientActor.java){ #publish }


Your client actor sends a [Publish](../messages.html#Publish) message to the router so to publish some data to the given topic. It might set the ``acknowledgment`` flag to ask the router to reply back upon publication. 

Your client actor receives either the [Error](../messages.html#Error) message from router if the publication fails or the [Published](../messages.html#Published) message if it succeeds (and the ``acknowledgment`` flag was set). In the latter case the router creates a new publication and delivers its identifier to your client actor via the published message.

Please refer to the [Payloads](../payloads.html) section for details about reading or writing payload contents.


### Subscribe to a topic
Your client actor generate the next request identifier and sends a [Subscribe](../messages.html#Subscribe) message to the router so to subscribe to a given topic.

Scala
:    @@snip [opening-scala](../../scala/docs/ScalaClientActor.scala){ #subscribe }

Java
:    @@snip [opening-java](../../java/docs/JavaClientActor.java){ #subscribe }

Your client actor receives either the [Error](../messages.html#Error) message from router if the subscription fails or the [Subscribed](../messages.html#Sbscribed) message if it succeeds. In the latter case the router creates a new subscription and delivers its identifier to your client actor via the subscribed message.


### Register a procedure
TBD

### Call a procedure
TBD


## Subscribed state
TBD 

### Consume events
TBD

## Registered state
TBD 

### Process invocations
TBD


