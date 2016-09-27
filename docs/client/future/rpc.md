# Remote Procedure Calls
The WAMP protocol defines a routed RPC mechanism which relies on the same sort of decoupling that is used by the [Publish Subscribe](../future/pubsub) communication pattern. 

A client, the __Callee__, announces to the router that it provides a certain procedure, identified by a procedure name.  Other clients, __Callers__, can then call the procedure, with the router invoking the procedure on the callee, receiving the procedure's result, and then forwarding this result back to the caller. Routed RPC differ from traditional client-server RPC in that the router serves as an intermediary between the caller and the callee.

```text
   ,------.          ,------.               ,------.
   |Caller|          |Dealer|               |Callee|
   `--+---'          `--+---'               `--+---'   
      |                 |             REGISTER |
      |                 | <--------------------|
      |                 |                      |
      |                 | REGISTERED           |
      |                 | -------------------->|
      |                 |                      |
      | CALL            |                      |
      |---------------->|                      |
      |                 | INVOCATION           | 
      |                 |--------------------->|
      |                 |                      |
      |                 |                YIELD |
      |                 |<---------------------|
      |                 |                      |
      |          RESULT |                      |
      |<----------------|                      |
   ,--+---.          ,--+---.               ,--+---.
   |Caller|          |Dealer|               |Callee|
   `------'          `------'               `------'
```
                                                                      
Akka Wamp provides you with an [Future](http://docs.scala-lang.org/overviews/core/futures.html) based API to easily write both kind of clients: callees (those registering procedures) and callers (those calling them).


## Register procedures
Once you got a callee session as documented in the [Session Handling](../future/session) section, you can finally register a procedure and its invocation handler:

```scala
// val session = ... 

val registration: Future[Registration] = session.flatMap(
  _.register(
    procedure = "myapp.procedure.sum") {
    invocation =>
      // echo input payload back to the caller  
      Future.successful(invocation.payload.map(identity))
  })    
```

A (future of) session can be mapped to a (future of) registration by just invoking the ``register`` method. It is a curried method with two parameter lists.

```scala
// as defined by Akka Wamp
def register(procedure: Uri)(handler: InvocationHandler)
```

The first parameter list accepts ``procedure`` as documented for the [``Register``](../../messages#Register) message constructor. The second parameter list accept a callback function of type ``InvocationHandler``.


### Invocation handlers
```scala
// as defined by Akka Wamp
type InvocationHandler = (Invocation) => Future[Option[Payload]]
```

The invocation handler is a function than transforms an invocation object to an (option of future of) output payload object. The invocation object comes with an (option of) input ``payload`` bearing (future of) application ``arguments``.
 
```scala
val handler: InvocationHandler = { invocation =>
  invocation.payload match {
    case Some(p) =>
      p.arguments.map { args =>
        val sum = args.map(_.asInstanceOf[Int]).sum
        Some(Payload(sum))
      }
    case None => 
      Future.successful(None)
}
```

Arguments can be received (from input payload) and sent (to output payload) as documented in the [Payload Handling](./payload) section.


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

val unregisterd: Future[Unregisterd] = session.flatMap(
    _.unregister("myapp.procedure.sum")
  )
```


### Recover
TBD

## Call procedures
Once you got a caller session as documented in the [Session Handling](../future/session) section, you can finally call a procedure:

```scala
// val session = ... 

val result: Future[Result] = session.flatMap(
  _.call(
    procedure = "myapp.procedure.sum",
    payload = Some(Payload(40, 4)))
)   
```

### Recover
TBD


