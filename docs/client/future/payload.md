# Payload Handling
WAMP messages, formatted as textual JSON or binary MsgPack, are transmitted by an underlying transport like WebSocket or RawTCP. Messages are all made of few headers and an optional payload. 

```text
frame2                  frame1                             
.-------------.        .-------------,---------.    
| payload ... |  -->   | ... payload | headers |  -->  
'-------------'        '-------------`---------'     
```


The payload contains application data either as a list or as a dictionary of arguments. The payload is always at the end of the message and for some applications (such a BigData or Multimedia Streaming applications), it could be of such a big size that some transports have to split messages in frames.
 


## Receive payloads

Akka Wamp provides efficient deserializers, implemented using streaming technologies, which do not eagerly parse the contents of incoming payloads but rather return lazy structures to defer parsing to the very last moment.  That lazy structures in question are implemented internally as ``LazyTextPayload`` and ``LazyBinaryPayload`` types.

```text
,-------------.  holds     ,-----------.
|PayloadHolder|---------->>|  Payload  |
`-------------'            `-----+-----'
                                 ^
                                 | inherits
                   .-------------+-------------. 
                   |                           |
           ,-------+-------.          ,--------+--------.
           |LazyTextPayload|          |LazyBinaryPayload|
           `---------------'          `-----------------'
```

Incoming messages that can hold a lazy payload are summarized by the following table alongside the client role that receives them: 

 Message       | Receiver    | Handler                  
---------------|-------------|------------------ 
``Event``      | Subscriber  | ``EventHandler``         
``Invocation`` | Callee      | ``InvocationHandler``      
``Result``     | Caller      |                        
    

### Access parsed data/arguments
You can let Akka Wamp lazily parse the content of incoming payloads on behalf of your client. Your subscriber, callee or caller can access data/arguments parsed by Akka Wamp as (future of) Scala collections of arbitrary types but deserialized with default data type bindings. 

For example, a subscriber client can access parsed event data as follows:
             
```scala
val handler: EventHandler = { event =>
  val data: Future[List[Any] = event.data
  val kwdata: Future[Map[String, Any]] = event.kwdata
    
  // ...
}
```
 
Similarly, a callee client can access parsed invocation arguments as follows:

```scala
val handler: InvocationHandler = { invocation =>
  val args: Future[List[Any] = invocation.args
  val kwargs: Future[Map[String, Any]] = invocation.kwargs
  
  // ...
}
```


#### Default data type bindings
By default, Akka Wamp makes use of either the embedded [Jackson JSON Parser](https://github.com/FasterXML/jackson-module-scala) (for textual data) or the embedded [MsgPack Parser](https://github.com/msgpack/msgpack-scala) (for binary data) and deserializes incoming data/arguments to Scala collection whose values match the following data type bindings:
 
JSON       | MsgPack   | Scala
-----------|-----------|------------------------
string     |           | java.lang.``String``
number     |           | java.lang.``Integer``, java.lang.``Long``, java.lang.``Double``
object     |           | scala.collection.immutable.``Map[String, _]``
array      |           | scala.collection.immutable.``List[_]``
true       |           | java.lang.``Boolean``
false      |           | java.lang.``Boolean``
null       |           | scala.``None``


#### Custom data type bindings
TBD



### Access unparsed content

> WARNING  
> This is an experimental feature that has not been released yet!

Your clients (either of role subscriber, callee or caller) can access incoming unparsed payload content by consuming an Akka Stream source. It comes in handy for those applications exchanging huge amount of data. But bear in mind that you will be put in charge of parsing payload content appropriately:

```scala
import akka.stream.scaladsl._
import akka.wamp.serialization._

val handler: EventHandler = { event =>
  event.payload match {
    case payload: BinaryPayload =>
      log.warn("Unexpected binaray payload")
      
    case payload: TextPayload =>
      val source: Source[String] = payload.source
      // ... consume/parse the Akka Stream source ...
      ()
  }
  // ...
}
```

You can match the payload against ``TextPayload`` or ``BinaryPayload``, get its Akka Stream source, then create whatever Akka Stream graph you need (composing flows and sink) and finally run the stream to parse the payload contents at your best convenience.


### Examples
TBD

#### JSON
TBD

#### YAML
TBD

#### MsgPack
TBD

#### Apache Avro
TBD

## Send payloads
TBD

