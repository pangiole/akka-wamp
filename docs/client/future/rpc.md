# Remote Procedure Calls
```scala
object RpcApp extends App {

  import akka.wamp.client._
  import akka.wamp.serialization._
  val client = Client()
  
  implicit val ec = client.executionContext

  val data = 
    for {
      session <- client
        .openSession(
          url = "ws://localhost:8080/router",
          subprotocol = "wamp.2.json",
          realm = "akka.wamp.realm",
          roles = Set("callee"))
      registration <- session
        .register(
          procedure = "myapp.procedure")(
          _.args.map { args =>
            Payload(args)
          })
      result <- session
        .call(
          procedure = "myapp.procedure",
          args = List("paolo", 40, true))
      data <- result.data
    } 
    yield (data)

  data.map(println)
}
```

The WAMP protocol defines a routed Remote Procedure Call (RPC) mechanism which relies on the same sort of decoupling that is used by the [Publish Subscribe](../future/pubsub) communication pattern. 

A client, the callee, announces to the dealer router that it provides a certain procedure, identified by a procedure name.  Other clients, callers, can then call the procedure, with the dealer router invoking the procedure on the callee, receiving the procedure's result, and then forwarding this result back to the callers. Routed RPC differ from traditional client-server RPC in that the dealer router serves as an intermediary between the callers and the callee.

```
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
Once you got a callee session as documented in the [Session Handling](../future/session) section, you can finally register a procedure and its invocation handler:

```scala
// val session = ... 

val registration: Future[Registration] = session.flatMap(
  _.register(
    procedure = "myapp.procedure.echo") {
    invocation =>
      invocation.args.map( args =>
        Payload(args)
      )})
```

A (future of) session can be mapped to a (future of) registration by just invoking the ``register`` method. It is a curried method with two parameter lists.

```scala
// as defined by Akka Wamp
def register(procedure: Uri)(handler: InvocationHandler)
```

The first parameter list accepts ``procedure`` as documented for the [``Register``](../../messages#Register) message constructor. The second parameter list accepts a callback function of type ``InvocationHandler``.


### Invocation handlers
```scala
// as defined by Akka Wamp
type InvocationHandler = (Invocation) => Future[Payload]
```

The invocation handler is a function that transforms an invocation to a (future of) output payload. The invocation comes with an input payload whose content can be lazily parsed as input arguments.
 
```scala
// handler for "myapp.procedure.sum"
val handler: InvocationHandler = { invocation =>
  invocation.kwargs.map { kwargs =>
    val arg0 = kwargs("0").asInstanceOf[Int]
    val arg1 = kwargs("1").asInstanceOf[Int]
    Payload(List(arg0 + arg1))
  }}
```

Arguments can be received from the incoming payload and replied back as documented in the [Payload Handling](./payload) section.


### Recover
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
 


### Recover
TBD

## Call procedures
Once you got a caller session as documented in the [Session Handling](../future/session) section, you can finally call a procedure:

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



### Recover
TBD


