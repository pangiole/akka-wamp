# Payloads
WAMP [Messages](./messages.html), arranged as either textual or binary formats (e.g. JSON, MsgPack, etc.), are moved over the network by an underlying transport protocol such as WebSocket or (raw) TCP. 

Messages are made of control __headers__ (e.g. type code, request identifier, additional details, etc.) and an optional __payload__ whose content represents data specific to your application. Data conveyed by messages can be either a list of indexed arguments called just ``args``, or a dictionary of named arguments called ``kwargs``. 

```
frame2                  frame1                             
.-------------.        .-------------,---------.    
| payload ... |  -->   | ... payload | headers |  -->  
'-------------'        '-------------`---------'     
```

Payloads are always at the end of messages so that routers don't have to parse them. Infact, routers are not required to inspect payload contents to do their job, which is all about routing messages.

![payloads](payloads.png)

## Incoming data

Akka Wamp provides you with efficient streaming parsers which do not fully deserialize incoming data into memory. Rather, it provides you with __lazy__ representations to defer deserialization to the very last moment. The lazy representations in question are ``TextLazyPayload`` for textual data and ``BinaryLazyPayload`` for binary data.


The following table summarizes which are the incoming messages behaving like ``DataConveyor``s, which client roles can receive those messages and which consumer/handlers you shall provide to read their incoming data from the lazy payloads above.

 PayloadConveyor  | Client     | Consumer/Handler                  
------------------|------------|------------------------------------ 
 ``Event``        | Subscriber | ``(Event) => Unit``         
 ``Invocation``   | Callee     | ``(Invocation) => Future[Payload]``      
 ``Result``       | Caller     | ``(Result) => Unit``            
 ``Error``        | _all_      | _n.a._                         
    

 
### Arguments
If you do not need to fully control the deserialization process then you can easily access to incoming data as follows:

Scala
:    @@snip [ScalaClient](../scala/docs/FuturesScalaClient.scala){ #incoming-arguments }

Java
:    @@snip [FuturesJavaClient](../java/docs/FuturesJavaClient.java){ #incoming-arguments }

Data conveyed by incoming messages are accessible as:

* ``args: Future[List[Any]]``  
  a (future of) list of indexed arguments   
      
* ``kwargs: Future[Map[String, Any]]``   
  a (future of) dictionary of named arguments
       
* ``kwargs[T]: Future[T]``  
   a (future of) user defined type

#### Data types
Akka Wamp will take care of the deserialization process either with [Jackson JSON Parser](https://github.com/FasterXML/jackson-module-scala) for textual format or [MsgPack Parser](https://github.com/msgpack/msgpack-scala) for binary format. Those default parsers will apply the following data type bindings:

Value          | JSON / MsgPack  | Scala / Java
---------------|-----------------|------------------------
``"string"``   | string          | java.lang.``String``
``456``        | number          | java.lang.``Integer``
``2147483648`` | number          | java.lang.``Long``
``12.56``      | number          | java.lang.``Double``
``[]``         | array           | scala.collection.immutable.``List[Any]`` 
               |                 | java.util.``List<Object>``
``{}``         | object          | scala.collection.immutable.``Map[String, Any]``
               |                 | java.util.``Map<String, Object>``
``true``       | boolean         | java.lang.``Boolean``
``false``      | boolean         | java.lang.``Boolean``
``null``       | null            | ``null``


### Unparsed
If you want full control of the deserializing process the you can access to the **unparsed** data as follows:

Scala
:    @@snip [ScalaClient](../scala/docs/FuturesScalaClient.scala){ #incoming-payload }

Java
:    @@snip [FuturesJavaClient](../java/docs/FuturesJavaClient.java){ #incoming-payload }

Unparsed data is represented as Akka Stream sources you're put in charge to consume with whatever parser you might prefer to use for whatever format you know incoming data could have been arranged with (e.g. UBJSON, YAML, XML, ProtoBuf, etc.)

@@@ note { title='Opaque payload' }
Akka Wamp adheres to the _"opaque payload"_ principle according to which the router is expected NOT to parse and NOT to alter payload contents. That's intended to allow payloads to be formatted using formats different than the enclosing messages' formats.
@@@
  
  
## Outgoing
TBD

### Arguments
TBD

Scala
:    @@snip [ScalaClient](../scala/docs/FuturesScalaClient.scala){ #outgoing-arguments }

Java
:    @@snip [FuturesJavaClient](../java/docs/FuturesJavaClient.java){ #outgoing-arguments }


### Streams
TBD