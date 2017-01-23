# Futures
Akka Wamp provides you with
 
* object-oriented representations of WAMP [Messages](./messages.html),
* this Futures API built atop of  [Akka Wamp Actors](./actors.html) and the [Akka Futures](http://doc.akka.io/docs/akka/current/scala/futures.html)
 
All operations are provided as __direct method calls__ returning composable futures. You can write your client applications, in either Scala or Java, so to connect to routers, open sessions to attach realms, publish or subscribe to topics, consume events, register or call remote procedures and handle invocations in few liens of code.

@@@ note
This shall be considered an __higher level API__ when compared to [Akka Wamp Actors](./actors.html) as it doesn't require you to know anything about how WAMP [Messages](../messages.html) are exchanged by peers.
@@@



## Client
Clients are those peers that, indirectly, communicate each other through a router. The Akka Wamp client instance can be created as follows: 

Scala
:    @@snip [FuturesScalaClient](../../scala/docs/FuturesScalaClient.scala){ #client }

Java
:    @@snip [FuturesJavaClient](../../java/docs/FuturesJavaClient.java){ #client }

Just invoke the ``Client`` factory method and pass the following arguments:

 * ``system: ActorSystem``  
   Is the [Akka Actor System](http://doc.akka.io/docs/akka/current/general/actor-systems.html) the client needs to spawn actors and provide execution context to futures.
   

## Connections
Clients that wish to communicate each other shall connect to the same router. The Akka Wamp client can connect to a router as follows:
 
Scala
:    @@snip [FuturesScalaClient](../../scala/docs/FuturesScalaClient.scala){ #connect }

Java
:    @@snip [FuturesJavaClient](../../java/docs/FuturesJavaClient.java){ #connect }

Just invoke the ``connect`` method and pass the following arguments:

* ``endpoint: String``  
  Is the name of a configured endpoint (default is ``"local"``). Read the configuration section further below.
   
or the following arguments:

* ``address: String``  
  Is the address to connect to (e.g. ``"wss://hostname:8433/router"``)
  
* ``format: String``  
  Is the format of messages as exchanged the wire (e.g. ``"msgpack"``)


### Configuration

@@snip[application.conf](../../../../../core/src/main/resources/reference.conf){ #client }
      

### Disconnect
The Akka Wamp client makes a distiction between _deliberate_ and _accidental_ disconnections. In either cases, any action performed in disconnected state will make the client throw ``ClientException("Disconnected")``

#### Deliberate
Disconnection is requested on purpose as follows:

Scala
:    @@snip [FuturesScalaClient](../../scala/docs/FuturesScalaClient.scala){ #disconnect }

Java
:    @@snip [FuturesJavaClient](../../java/docs/FuturesJavaClient.java){ #disconnect }

Just invoke the ``disconnect`` method.

#### Accidental
Disconnection is not requested but suddenly happens (for example on mobile devices connected via wireless networks). The Akka Wamp client does not provide any mechanism to recover from this state. The connection object becomes useless and a new connection must be established.

@@@warning
Please join the ongoing _"[Session Resumption](https://github.com/wamp-proto/wamp-proto/pull/264/files)"_ discussion as some different behaviour proposals are under review.
@@@ 



## Sessions
A realm is a routing and administrative domain, optionally protected by authentication and authorization, that holds subscriptions to topics and registrations of procedures for all clients attached to it.

A session is a transient conversation between a client and a router, running over a transport connection, that starts when the client requests to be attached to a specific realm. Attaching to a realm is also referred as _"opening a session"_


### Open
Once got a (future of) connection, open a session over it so to attach the client to a specific realm.

Scala
:    @@snip [FuturesScalaClient](../../scala/docs/FuturesScalaClient.scala){ #open }

Java
:    @@snip [FuturesJavaClient](../../java/docs/FuturesJavaClient.java){ #open }

Just invoke the ``open`` method passing the following arguments:

* ``realm: Uri``  
   Is the realm name (default is ``"default"``)
      
        
### Close
Once the client doesn't need to keep the session attached, it can close it as follows:
  
Scala
:    @@snip [FuturesScalaClient](../../scala/docs/FuturesScalaClient.scala){ #close }

Java
:    @@snip [FuturesJavaClient](../../java/docs/FuturesJavaClient.java){ #close }
     
Just invoke the ``close`` method.

      
## Topics
The client either publish events or subscribe to topics.

### Publish
Once got a (future of) session, the client can publish an event to a topic with either _"fire and forget"_ or _"acknowldeged"_ pattern.

Scala
:    @@snip [FuturesScalaClient](../../scala/docs/FuturesScalaClient.scala){ #publish }

Java
:    @@snip [FuturesJavaClient](../../java/docs/FuturesJavaClient.java){ #publish }

Just invoke any the following overloaded methods:

* ``publish``  
    It publishes in _"fire and forget"_ pattern and returns no indication of what happened (neither failures).
   
    * ``topic: Uri``  
      Is the topic to publish to.
    
    * ``payload: Payload``    
      Is the outgoing event payload. Please refer to the [Payloads](../payloads.html#outgoing) section for further details.
            
* ``publishAck``  
    It publishes with _"acknowledged"_ pattern so to return a (future of) publication. It accepts the same arguments as above.
    
    
When publishing with _"acknowledged"_ pattern the client can provide callbacks to be invoked upon future completion so to test against success or failure.
 
Scala
:    @@snip [FuturesScalaClient](../../scala/docs/FuturesScalaClient.scala){ #publication-completion }

Java
:    @@snip [FuturesJavaClient](../../java/docs/FuturesJavaClient.java){ #publication-completion }
 

### Subscribe
Once got a (future of) session, the client can subscribe an lambda consumer to a topic as follows:

Scala
:    @@snip [FuturesScalaClient](../../scala/docs/FuturesScalaClient.scala){ #subscribe }

Java
:    @@snip [FuturesJavaClient](../../java/docs/FuturesJavaClient.java){ #subscribe }

Just invoke the ``subscribe`` method with the following arguments:
  
  * ``topic: Uri``  
    Is the topic to subscribe to
    
  * ``consumer``  
    Is a consumer as explained further below.
    
The client can provide callbacks to be invoked upon future completion so to test against success or failure.
 
Scala
:    @@snip [FuturesScalaClient](../../scala/docs/FuturesScalaClient.scala){ #subscription-completion }

Java
:    @@snip [FuturesJavaClient](../../java/docs/FuturesJavaClient.java){ #subscription-completion }
 
 
### Consumer
The client can subscribe any function able to consume incoming events. It can be either an event consumer or a lambda consumer as explained further below.

#### Lambda Consumer
The client can subscribe a lambda consumer that accepts as many parameters as it would expect to be conveyed by incoming events.

Scala
:    @@snip [FuturesScalaClient](../../scala/docs/FuturesScalaClient.scala){ #lambda-consumer }

Please refer to the [Macros](macros.html#register) section for further details about how to access arguments conveyed by incoming events.

@@@warning
Lambda consumers are supported for Scala only
@@@


#### Event Consumer
The client can subscribe an event consumer as a function that accepts exactly one argument of type [``Event``](../messages.html#event) and returns (future of) ``Done``.

Scala
:    @@snip [FuturesScalaClient](../../scala/docs/FuturesScalaClient.scala){ #event-consumer }

Java
:    @@snip [FuturesJavaClient](../../java/docs/FuturesJavaClient.java){ #event-consumer }


Please refer to the [Payloads](../payloads.html#arguments) section for details about how to access arguments conveyed by incoming events.

 

### Unsubscribe
Once got a (future of) subscription, the client can unsubscribe from it.

Scala
:    @@snip [FuturesScalaClient](../../scala/docs/FuturesScalaClient.scala){ #unsubscribe }

Java
:    @@snip [FuturesJavaClient](../../java/docs/FuturesJavaClient.java){ #unsubscribe }

Just invoke the ``unsubscribe`` method.



## Procedures
The client can either call or register remote procedures.

### Call
Once got a (future of) session, the client can call a remote procedure as follows

Scala
:    @@snip [FuturesScalaClient](../../scala/docs/FuturesScalaClient.scala){ #call }

Java
:    @@snip [FuturesJavaClient](../../java/docs/FuturesJavaClient.java){ #call }

Just invoke the ``call`` method with the following arguments:
  
  * ``procedure: Uri``  
    Is the remote procedure name to call
    
  * ``args``  
    Are the arguments to provide the invocation with
    
    
The client can provide callbacks to be invoked upon future completion so to test against success or failure.

Scala
:    @@snip [FuturesScalaClient](../../scala/docs/FuturesScalaClient.scala){ #result }

Java
:    @@snip [FuturesJavaClient](../../java/docs/FuturesJavaClient.java){ #result }


 
### Register
Once got a (future of) session, the client can register a local invocation handler as endpoint of a remote procedure as follows:

Scala
:    @@snip [FuturesScalaClient](../../scala/docs/FuturesScalaClient.scala){ #register }

Java
:    @@snip [FuturesJavaClient](../../java/docs/FuturesJavaClient.java){ #register }

Just invoke the ``register`` method with the following arguments:

* ``procedure: Uri``  
  Is the procedure to register.
  
* ``handler``  
  Is a handler as explained further below.
  
The client can provide callbacks to be invoked upon future completion to test against success or failure.
   
Scala
:    @@snip [FuturesScalaClient](../../scala/docs/FuturesScalaClient.scala){ #registration }

Java
:    @@snip [FuturesJavaClient](../../java/docs/FuturesJavaClient.java){ #registration }
   


### Handler
The client can register any function able to handle incoming invocations. It can be either an invocation handler or a lambda handler as explained further below.

#### Lambda Handler
The client can register a lambda handler that accepts as many parameters as you would expect to be conveyed by incoming invocations. 

Scala
:    @@snip [FuturesScalaClient](../../scala/docs/FuturesScalaClient.scala){ #lambda-handler }

Please refer to the [Macros](macros.html#register) section for further details about how to access arguments conveyed by incoming invocations.

@@@warning
Lambda handlers are supported for Scala only.
@@@


#### Invocation Handler
The client can subscribe an invocation handler as a function that accepts exactly one argument of type [``Invocation``](../messages.html#invocation) and returns a (future of) ``Payload``.

Scala
:    @@snip [FuturesScalaClient](../../scala/docs/FuturesScalaClient.scala){ #invocation-handler }

Java
:    @@snip [FuturesJavaClient](../../java/docs/FuturesJavaClient.java){ #invocation-handler }

Please refer to the [Payloads](../payloads.html#arguments) section for futher details about how to access arguments conveyed by incoming invocations.



### Unregister
Once got a (future of) registration, the client can unregister from it.

Scala
:    @@snip [FuturesScalaClient](../../scala/docs/FuturesScalaClient.scala){ #unregister }

Java
:    @@snip [FuturesJavaClient](../../java/docs/FuturesJavaClient.java){ #unregister }

Just invoke the ``unregister`` method.



## Putting all together

Scala
:    @@snip [FuturesScalaClient](../../scala/docs/FuturesScalaClient.scala){ #all-together }

Java
:    @@snip [FuturesJavaClient](../../java/docs/FuturesJavaClient.java){ #all-together }


