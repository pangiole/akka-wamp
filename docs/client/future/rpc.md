# Remote Procedure Calls
```scala
import akka.wamp.client._
import akka.wamp.serialization._

object MyClientApp extends App {
  val client = Client()  
  implicit val ec = client.executionContext

  val data = 
    for {
      session <- client
        .openSession(
          url = "ws://localhost:8080/ws",
          realm = "myapp.realm")
      registration <- session
        .register(
          procedure = "myapp.procedure.sum",
          handler = (a: Int, b: Int) => a + b)
      result <- session
        .call(
          procedure = "myapp.procedure.multiply",
          args = List(23.5, 12.89))
      data <- result.data
    } 
    yield data

  data.map(println)
}
```

The WAMP protocol defines a routed Remote Procedure Call (RPC) mechanism which relies on the same sort of decoupling that is used by the [Publish Subscribe](../future/pubsub) communication pattern. 

A client, the callee, announces to the dealer router that it provides a certain procedure, identified by a procedure name.  Other clients, callers, can then call the procedure, with the dealer router invoking the procedure on the callee, receiving the procedure's result, and then forwarding this result back to the callers. Routed RPC differ from traditional client-server RPC in that the dealer router serves as an intermediary between the callers and the callee.

```text
   ,------.          ,------.               ,------.
   |Caller|          |Dealer|               |Callee|
   `--+---'          `--+---'               `--+---'   
      |                 |             REGISTER |
      |                 <<---------------------|
      |                 |                      |
      |                 | REGISTERED           |
      |                 |--------------------->>
      |                 |                      |
      | CALL            |                      |
      |---------------->>                      |
      |                 |                      |
      |                 | INVOCATION           | 
      |                 |--------------------->>
      |                 |                      |
      |                 |                YIELD |
      |                 <<---------------------|
      |                 |                      |
      |          RESULT |                      |
      <<----------------|                      |
   ,--+---.          ,--+---.               ,--+---.
   |Caller|          |Dealer|               |Callee|
   `------'          `------'               `------'
```
                                                                      
Akka Wamp provides you with an [Future](http://docs.scala-lang.org/overviews/core/futures.html) based API to easily write both kind of clients: callee (those registering procedures) and callers (those calling them).


## Register procedures
Once you got a session, you can register procedures:

```scala
// val session = ... 

for {
  ssn <- session
  registration1 <- ssn.register(
    procedure = "myapp.procedure.sum",
    handler = (a: Int, b: Int) => a + b)
  registration2 <- ssn.register(
    procedure = "myapp.procedure.echo",
    handler = { i: Invocation => i.args.map(Payload(_)) })
} 
yield ()
```

A (future of) session can be mapped to a (future of) registration by invoking one of the provided ``register()`` methods. 

```scala
// passing any of the scala.Function as handler
def register[F](procedure: Uri, handler: F)

// passing an AkkaWamp invocation handler
def register(procedure: Uri, handler: InvocationHandler)
```

They accept 

* ``procedure`` URI as documented for the [``Register``](../../messages#Register) message
* and they accept either      
    * any of the ``scala.Function`` types
    * or an ``akka.wamp.InvocationHandler``


### Handle with ``scala.Function``
As you probably know, following are some alternative ways to define functions and partially applied functions in Scala. 

```scala
// equivalent functions
def sum (a: Int, b: Int): Int = a + b
val total = (a: Int, b: Int): Int => a + b
val add: Function2[Int, Int, Int] = a + b
val plus: (Int, Int) => Int = a + b

// partially applied functions
val a = add(8, _)
val p = plus(_, 12)
val s = sum _
```

You can register a procedure URI with either an anonymous function or a named partially applied function as handler.

```scala
val registration1: Future[Registration] = session.flatMap(
  _.register(
    procedure = "myapp.procedure.sum1",
    handler = (Int, Int) => Int = a + b ))
    
val registration2: Future[Registration] = session.flatMap(
  _.register(
    procedure = "myapp.procedure.sum2",
    handler = sum _ ))
```

Akka Wamp will dinamically invoke the function you provide as handler (e.g. ``sum _``) upon receiving invocations addressed to the registered procedure URI (e.g. ``"myapp.procedure.sum"``). It will lazily deserialize the ``args`` list from incoming payloads by using its default type bindings as documented in the [Payload Handling](./payload/#default-type-bindings) section. 

> Notice  
> Lazy deserialization from incoming ``kwargs`` and dynamic invocation passing input values to named arguments is not yet supported (see [issue/41](https://github.com/angiolep/akka-wamp/issues/41))


### Handle with ``InvocationHandler``
An invocation handler is a function that transforms an [``Invocation``](../../messages#Invocation) message to a (future of) ``Payload`` to be replied back to the caller in a [``Result``](../../messages#Invocation) message.

```scala
type InvocationHandler = (Invocation) => Future[Payload]
```

You can register a procedure URI and provide your custom invocation handler. Doing that you'll be put in charge of deserializing either the ``args`` list or the ``kwargs`` dictionary from the incoming payload 

```scala
// handler for "myapp.procedure.sum"
val handler: InvocationHandler = { invocation =>
  invocation.kwargs.map { kwargs =>
    val a = kwargs("a").asInstanceOf[Int]
    val b = kwargs("b").asInstanceOf[Int]
    Payload(List(a + b))
  }}
```

Though more complicated than handling with ``scala.Function``, this technique gives you much more flexibility because you can 

* deserialize by applying custom type bindings (e.g. deserialize your custom application types rather than primitive ones) or 
* even deserialize formats different than JSON/MsgPack (e.g. you can deserialize Apache Avro or Google Protocol Buffer if you wish!) 

Please read the [Payload Handling](./payload) section for further details.



### Recover exceptions
You can either recover or _"give up"_ when the (future of) registration fails. To recover from failures (such as ``SessionException`` thrown when the procedure has been already registered by some other callee or when the session turns out to be closed) you can compose ``recoverWith`` to attempt registering the procedure with a different name or to attempt another session (maybe to a fallback realm):

```scala
val registration = session.flatMap(
  _.register("myapp.procedure.sum")(handler)
  .recoverWith { 
    case ex: SessionException => session.flatMap(
      _.register("myapp.procedure.sum.renamed")(handler)
  }
```

As last resort, instead of recovering, you could decide to _"give up"_ a callback function ``onFailure`` that just prints a log message:

```scala
session.onFailure {
  case ex: Throwable => 
    log.error(ex.getMessage, ex)
}
```

## Unregister procedures

```scala
import akka.wamp.messages._

val registration: Future[Registration] = ...

val unregistered: Future[Unregistered] = registration.flatMap(
  _.unregister()
)
```

Just call the ``unregister()`` method on the registration you want to terminate.
 


### Recover exceptions
TBD

## Call procedures
Once you got a session, as documented in the [Session Handling](../future/session) section, you can call a procedure:

```scala
// val session = ... 

val result: Future[Result] = session.flatMap(
  _.call(
    procedure = "myapp.procedure.sum",
    args = List(2, 4, 8, 12))
)

val data: Future[List[Any]] = result.flatMap(
  _.data
)
```

Arguments can be received from the resulting payload as documented in the [Payload Handling](./payload) section.



### Recover exceptions
TBD


