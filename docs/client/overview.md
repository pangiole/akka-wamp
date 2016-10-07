# Client APIs overview
Akka Wamp provides you with three alternative Client APIs:

* [Actor based API](../client/actor/overview)
* [Future based API](../client/future/overview)
* [Stream based API](../client/stream)

They all provide easy ways to establish transports and sessions, publish and subscribe topics, register and call procedures. They differ in terms of which [Akka](http://doc.akka.io/docs/akka/current/intro/what-is-akka.html) abstractions you'd prefer to use in writing client applications.
 


## Transports and Sessions
The WAMP protocol specification states:

> WAMP implementations MAY choose to tie the lifetime of the underlying transport connection for a WAMP connection to that of a WAMP session, i.e. establish a new transport-layer connection as part of each new session establishment.
     
That's how many WAMP implementations (such AutobahnJS, Crossbar.io, Jawampa, etc.) actually work. But, as you read the spec a little bit further:

>They MAY equally choose to allow re-use of a transport connection, allowing subsequent WAMP sessions to be established using the same transport connection.
      
That's how Akka Wamp works, instead! 

Akka Wamp handles transport connections and sessions as separate entities, each with their own lifecycle. That gives you a way to keep the transport connection established whatever happens to the session that has been opened onto it. So that, if the session gets closed you can still reuse the transport connection to establish a subsequent new session.

For example, using the future based API, you could:

```scala
val conn = client.connect()
val session1 = conn.flatMap(_.openSession())
// ... 
session1.close()

// reuse the same connection to establish subsequent session
val session2 = conn.flatMap(_.openSession())
```


## Read on

* [Actor based API](../client/actor/overview)
* [Future based API](../client/future/overview)
* [Stream based API](../client/stream)