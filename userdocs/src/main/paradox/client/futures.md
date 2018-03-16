# Futures
@@@ note
This is an *high level* API which does **not** require deep knowledge of the WAMP protocol and is meant to be used by anyone. If you're a WAMP protocol implementor and wish to fully control the underlying exchange of messages, then [Akka Wamp Actors](actos.html) might be a better option for you.
@@@


Akka Wamp Futures is Client API which provides you with
 
* object-oriented representations of all WAMP [Messages](./messages.html),
* an easy to use collection of classes built atop of  [Akka Wamp Actors](./actors.html) and [Akka Futures](http://doc.akka.io/docs/akka/current/scala/futures.html)
 
All operations are provided as __direct method calls__ returning composable futures. You can write your client applications, in either Scala or Java, so to connect to routers, open sessions to join realms, publish or subscribe to topics, consume events, register or call remote procedures and handle invocations ... all in few lines of code!




## Client
According to the WAMP protocol, clients are those entities that communicate each other through remote routers. This API let your create the client instance for your application as shown below: 

Scala
:    @@snip [FuturesScalaClient](../../scala/docs/FuturesScalaClient.scala){ #client }

Java
:    @@snip [FuturesJavaClient](../../java/docs/FuturesJavaClient.java){ #client }

Just invoke the ``Client`` factory method and pass in the following arguments:

 * ``system: ActorSystem``  
   Is the Akka [Actor System](http://doc.akka.io/docs/akka/current/general/actor-systems.html) needed by the client not only for the purpose of spawning its internal Akka actors, but also to obtain a default [execution context](https://doc.akka.io/docs/akka/current/futures.html#execution-contexts) for the Akka Futures that will be created next.
   
Be aware that the client instance is created once and then shared every where in your application you needed to establish connections to the remote routers.


## Connections
According to the WAMP protocol, clients willing to communicate each other shall connect to the same router. This API let your client connect to a given router by passing its endpoint name as shown below:
 
Scala
:    @@snip [FuturesScalaClient](../../scala/docs/FuturesScalaClient.scala){ #connect }

Java
:    @@snip [FuturesJavaClient](../../java/docs/FuturesJavaClient.java){ #connect }

Just invoke the ``connect`` method and pass in the following arguments:

* ``endpoint: String``  
  Is the name of a configured endpoint (default is ``"default"``). Read the [configuration](./config.html) page to better understand how a configured endpoint can summarize the address, the message format and other properties of the remote router. 
   
   

### Disconnect
This API makes a fundamental distinction between _deliberate_ and _accidental_ disconnections. In either cases, once a disconnection happens, any subsequent action performed during the disconnected state will make the client throw ``ClientException("Disconnected")``

#### Deliberate
Disconnection might be deliberately demanded as shown below:

Scala
:    @@snip [FuturesScalaClient](../../scala/docs/FuturesScalaClient.scala){ #disconnect }

Java
:    @@snip [FuturesJavaClient](../../java/docs/FuturesJavaClient.java){ #disconnect }

Just invoke the ``disconnect`` method onto the connection object.


#### Accidental
Disconnection might happen by accident without willingness. For example when your application runs on a mobile device with poor network signal. This API does __not__ provide any mechanism to recover from accidental disconnections. The connection object becomes useless and you'll be required to establish a new one.

@@@warning
Please join the ongoing _"[Transport Resumption](https://github.com/wamp-proto/wamp-proto/issues/293)"_ discussion as some new proposals are under review.
@@@ 



## Sessions
According to the WAMP protocol, once connected to the same router, clients willing to communicate each other shall open a session to join the same realm.
 
A __session__ is a transient conversation between a client and a router, running over a transport connection, a session begins when the client requests to be joined to a specific realm.  A __realm__ is a routing and administrative domain, optionally protected by authentication and authorization, that holds subscriptions to topics and registrations of procedures for all clients joined to it. Joining to a realm is also referred as _"opening a session"_


### Open
Once got a (future of) connection, open a session over it so to join the client to a given realm.

Scala
:    @@snip [FuturesScalaClient](../../scala/docs/FuturesScalaClient.scala){ #open }

Java
:    @@snip [FuturesJavaClient](../../java/docs/FuturesJavaClient.java){ #open }

Just invoke the ``open`` method passing the following arguments:

* ``realm: Uri``  
   Is the realm name (default is ``"default"``) to join
      
        
### Close
Once the client doesn't need to keep the session opened, it can close it as shown below:
  
Scala
:    @@snip [FuturesScalaClient](../../scala/docs/FuturesScalaClient.scala){ #close }

Java
:    @@snip [FuturesJavaClient](../../java/docs/FuturesJavaClient.java){ #close }
     
Just invoke the ``close`` method and all subcriptions/registrations held for that client will be immediately disposed by the remote router.

      
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
Once got a (future of) session, the client can subscribe an lambda consumer to a topic as shown below:

Scala
:    @@snip [FuturesScalaClient](../../scala/docs/FuturesScalaClient.scala){ #subscribe }

Java
:    @@snip [FuturesJavaClient](../../java/docs/FuturesJavaClient.java){ #subscribe }


Just invoke the ``subscribe`` method with the following arguments:
  
  * ``topic: Uri``  
    Is the topic to subscribe to
    
  * ``consumer``  
    Is a consumer callback as explained further below.
    
The client can provide callbacks to be invoked upon future completion so to test against success or failure.
 
Scala
:    @@snip [FuturesScalaClient](../../scala/docs/FuturesScalaClient.scala){ #subscription-completion }

Java
:    @@snip [FuturesJavaClient](../../java/docs/FuturesJavaClient.java){ #subscription-completion }
 
 
### Consumer
The client can subscribe any callback function given either as an event consumer or as _lambda_ consumer.

Please note that, as this API is build atop of [Akka Wamp Actors](../client/actors.html), your callback function will be invoked in the same thread which delivers the [Event](../messages.html) message from underlying actor's mailbox. Therefore, it is safe to close your callback over free variables as there's no risk to have multiple threads executing the handler at the same time.


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
Once got a (future of) session, the client can call a remote procedure as shown below

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
Once got a (future of) session, the client can register a local invocation handler as endpoint of a remote procedure as shown below:

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
The client can register any callback function given either as an invocation handler or as _lambda_ handler.

Please note that, as this API is build atop of [Akka Wamp Actors](../actors.html), your callback function will be invoked in the same thread which delivers the [Invocation](../messages.html) message from underlying actor's mailbox. Therefore, it is safe to close your callback over free variables as there's no risk to have multiple threads executing the handler at the same time.

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


