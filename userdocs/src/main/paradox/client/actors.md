# Actors

@@@ warning 
This is a *low level* API which requires a deep knowledge of the WAMP protocol and is meant to be used by protocol implementors. If you're just an ordinary user then the [Akka Wamp Futures](futures.html) based API is a much better option for you.
@@@

Akka Wamp Actors is Client API which provides you with

 * object-oriented representations of all WAMP [Messages](./messages.html),
 * an [Akka I/O](http://doc.akka.io/docs/akka/current/scala/io.html) extension driver named ``akka.wamp.Wamp`` that takes care of low-level network communication with the remote router,
 * abstract base classes your client actor can extend from.

All operations are implemented with __message passing__ instead of direct method calls. You can write your own client actor, in either Scala or Java, to connect to a router, open a session, publish or subscribe to topics, consume events, process invocations and register or call procedures.


## Client

Scala
:    @@snip [ActorsScalaClient.scala](../../scala/docs/ActorsScalaClient.scala){ #client }

Java
:    @@snip [ActorsJavaClient.java](../../java/docs/ActorsJavaClient.java){ #client }

You client actor starts as _"disconnected from router"_.


### Connect to a router
While in ``initial`` state, your client actor sends a [Connect](../messages.html#Connect) command to the Akka Wamp [extension manager](./index.html#extension-manager) so to attempt a new connection to the router addressed by the given URI. 

Scala
:    @@snip [ActorsScalaClient.scala](../../scala/docs/ActorsScalaClient.scala){ #connect }

Java
:    @@snip [ActorsJavaClient.java](../../java/docs/ActorsJavaClient.java){ #connect }

Your client receives either the [CommandFailed](../messages.html#CommandFailed) signal from the manager if the connection attempt fails or the [Connected](../messages.html#Connected) signal if it succeeds. In the latter case the manager spawns a new worker actor representing the handler for that specific connection and delivers its reference to your client via the signal.

Upon receiving the [Connected](../messages.html#Connected) signal, your client actor stores the connection handler reference, holding it as the ``router`` representative, and switches its state to ``connected``. 



## In ``connected`` state
Your client actor is _"connected to a router"_.

### Handle disconnection
Your connected client actor might receive the [Disconnected](../messages.html#Disconnected) signal from the manager actor if the connection disconnects. For example, clients on mobile devices could experience accidental disconnections because of wireless networks unavailability. Or, the router could deliberately shutdown. 

When disconnection happens, you client actor might keep attempting to establish a new connection, then open a new session and try to restore its subscriptions/registrations.

 
@@@ warning
See https://github.com/angiolep/akka-wamp/issues/44
@@@ 

### Open a session
Your client actor sends an [Hello](../messages.html#Hello) message to the router to ask for a new session to be joined to the given realm (for example the ``default`` realm).
 
Scala
:    @@snip [opening-scala](../../scala/docs/ActorsScalaClient.scala){ #open }

Java
:    @@snip [opening-java](../../java/docs/ActorsJavaClient.java){ #open }


Your client actor receives either the [Abort](../messages.html#Abort) message or the [Welcome](../messages.html#Welcome) message. In the former case, the router rejects the session opening, while in the latter case the router creates a new session and delivers its identifier to your client.

Upon receiving the [Welcome](../messages.html#Welcome) message, your client can switch to the ``open`` state. 


## In ``open`` state
You client actor now _"holds an open session"_. In this state it can finally publish or subscribe to topics, process incoming events, register procedures, process incoming invocations, call procedures, etc.
 
### Handle session close
In addition to [unexpected disconnections](#disconnection), your client actor might receive the [Goodbye](../messages.html#Goodbye) message from router if session gets closed for some reason. 

If that happens, you client actor might keep attempting to open a new session so to restore its subscriptions/registrations ... 


### Publish to a topic
Scala
:    @@snip [opening-scala](../../scala/docs/ActorsScalaClient.scala){ #publish }

Java
:    @@snip [opening-java](../../java/docs/ActorsJavaClient.java){ #publish }


Your client actor sends a [Publish](../messages.html#Publish) message to the router so to publish some data to the given topic. It might set the ``acknowledgment`` flag to ask the router to reply back upon publication. 

Your client actor receives either the [Error](../messages.html#Error) message from router if the publication fails or the [Published](../messages.html#Published) message if it succeeds (and the ``acknowledgment`` flag was set). In the latter case the router creates a new publication and delivers its identifier to your client actor via the published message.

Please refer to the [Payloads](../payloads.html) section for details about reading or writing payload contents.


### Subscribe to a topic
Your client actor generate the next request identifier and sends a [Subscribe](../messages.html#Subscribe) message to the router so to subscribe to a given topic.

Scala
:    @@snip [opening-scala](../../scala/docs/ActorsScalaClient.scala){ #subscribe }

Java
:    @@snip [opening-java](../../java/docs/ActorsJavaClient.java){ #subscribe }

Your client actor receives either the [Error](../messages.html#Error) message from router if the subscription fails or the [Subscribed](../messages.html#Sbscribed) message if it succeeds. In the latter case the router creates a new subscription and delivers its identifier to your client actor via the subscribed message.


### Register a procedure
TBD

### Call a procedure
TBD

### Consume events
TBD

### Process invocations
TBD


