# Remote Procedure Calls
TODO

## Register procedures
```scala
import akka.wamp._
import akka.wamp.client._
import scala.concurrent._

// implicit val executionContext = ...
// val session = ... 

val registration: Future[Registration] = session.flatMap(
  _.register(
    procedure = "myapp.procedure",
    options = Dict()) {
    invocation =>
      log.info(s"${invocation.registrationId}")
      invocation.payload.map(_.arguments.map(println))
    })
```

A (future of) session can be mapped to a (future of) registration by just invoking the ``register`` method. It is a curried method with two parameter lists.

```scala
def register(procedure: Uri, options: Dict)(handler: InvocationHandler)
```

The first parameter list accepts ``procedure`` and ``options`` arguments as documented for the [``Register``](../../messages#Register) message constructor. The second parameter list accept a callback handler function of type ``InvocationHandler`` which gets invoked to process each invocation for the procedure. 

The ``invocation`` object provides a (option of) ``payload`` which in turn provides both (future of) ``arguments`` and ``argumentsKw``. Receiving arguments is better explained in the [Payload Handling](./payload) section. 

### Recover
You can either recover or _"give up"_ when the (future of) registration fails. To recover from failures (such as ``SessionException`` thrown when the procedure has been already registered by some other callee or when the session turns out to be closed) you can compose ``recoverWith`` to attempt registering the procedure with a different name or to attempt another session (maybe to a fallback realm):

```scala
val registration = session.flatMap(
  _.register("myapp.procedure")(handler)
  .recoverWith { 
    case ex: SessionException => session.flatMap(
      _.register("myapp.procedure.renamed")(handler)
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
    _.unregister("myapp.procedure")
  )
```


### Recover
TBD

## Call procedures
TBD

### Recover
TBD


