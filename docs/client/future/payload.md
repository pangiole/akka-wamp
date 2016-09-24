# Payload Handling
WAMP messages, formatted as textual JSON or binary MsgPack, are transmitted by an underlying transport such as WebSocket or RawTCP. They're all made of few headers and an optional payload. 

The payload contains application data as arguments and it is always at the end of the message. In some cases, it could be of such a big size that some transports have to split messages in frames.
 
```text
  frame2                  frame1                             
  .-------------.        .-------------,---------.    
  | payload ... |  -->   | ... payload | headers |  -->  
  '-------------'        '-------------`---------'     
```


## Receiving arguments

Akka Wamp provides efficient deserializers, implemented using streaming technologies, which do NOT parse incoming payloads but rather return lazy structures to defer deserialization to the very last moment. 

That lazy structure in question is implemented by the ``Payload`` class and the messages containing it are represented by the following case classes: 

* ``Error``  
* ``Event``  
* ``Invocation``

which mixin both the ``PayloadContainer`` and ``ArgumentExtractor`` traits. 


### As Scala collections

Your client (subscriber or callee) can extract arguments from the incoming message payload as Scala collections and let Akka Wamp parse them using the most appropriate parser:

```scala
import scala.concurrent.Future
import akka.wamp.client.InvocationHandler

val handler: InvocationHandler = { invocation =>
  val args: Future[List[Any]] = invocation.arguments
  val argsKw: Future[Map[String, Any]] = invocation.argumentsKw
  // ... map args to results ...
}
```

Akka Wamp uses either the default JSON parser (for textual data) or the default MsgPack parser (for binary data) and returns a collection of values of arbitrary types. 

### Via Akka streams
You can access to the payload contents as an Akka Stream source. Bear in mind that you will be put in charge of consuming the stream so to parse its contents using whatever parser:

```scala
import akka.stream.scaladsl._
import akka.wamp.serialization._

val handler: EventHandler = { event =>
  val arguments = event.payload match {
    case Some(payload: TextPayload) =>
      val source: Source[String] = payload.source()
      // see Akka Stream docs ...
      ???
            
    case Some(payload: BinaryPayload) =>
      val source: Source[ByteString] = payload.source()
      ???
      
    case None => ???
  }
  // ...
}
```

You can match the payload against ``TextPayload`` or ``BinaryPayload``, get its source, then create whatever Akka Stream graph you need (composing flows and sink) and finally run it to parse the payload arguments at your best convenience.


### Examples
TBD

#### YAML
TBD

#### Apache Avro
TBD

## Sending arguments
TBD

