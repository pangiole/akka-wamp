# Payloads
WAMP [Messages](./messages.html), arranged as either textual or binary formats (e.g. JSON, MsgPack, etc.), are moved over the network by an underlying transport protocol such as WebSocket or (raw) TCP. 

Messages are made of control __headers__ (e.g. type code, request identifier, additional details, etc.) and an optional __payload__ whose content represents data specific to your application. Data conveyed by messages can be either a list of indexed arguments called just ``args``, or a dictionary of named arguments called ``kwargs``. 

```
frame2                  frame1                             
.-------------.        .-------------,---------.    
| payload ... |  -->   | ... payload | headers |  -->  
'-------------'        '-------------`---------'     
```

Payloads are always at the end of messages so that routers don't have to parse them. In fact, routers are not required to inspect payload contents to do their job, which is all about routing messages.

![messages](messages.png)

## Incoming data

Akka Wamp provides you with efficient streaming parsers that defer deserialization of incoming data to the very last moment. 

The following table lists incoming [Message](./messages.html)s behaving as ``DataConveyor``s with the correspondent consumers/handlers and client roles you shall provide to extract the data.

 DataConveyor     | Consumer/Handler        | Client     
------------------|-------------------------|------------
 ``Event``        | ``(Event) => Unit``     | Subscriber 
 ``Invocation``   | ``(Invocation) => Any`` | Callee     
 ``Result``       | ``(Result) => Unit``    | Caller     
 ``Error``        | _n.a._                  | _all_      
    

 
### Arguments
The easiest way to access data conveyed by incoming messages is as follows:

Scala
:    @@snip [ScalaClient](../scala/docs/FuturesScalaClient.scala){ #incoming-arguments }

Java
:    @@snip [FuturesJavaClient](../java/docs/FuturesJavaClient.java){ #incoming-arguments }

Just access the following members:

* ``args: List[Any]``  
  a list of indexed arguments   
      
* ``kwargs: Map[String, Any]``   
  a dictionary of named arguments
       
* ``kwargs[T]: T``  
   a user defined type

#### Data types
Akka Wamp will take care of the deserialization process either with [Jackson JSON Parser](https://github.com/FasterXML/jackson-module-scala) for textual format or [MsgPack Parser](https://github.com/msgpack/msgpack-scala) for binary format. The default parsers will apply the following data type bindings:

Value          | JSON / MsgPack  | Scala / Java
---------------|-----------------|------------------------
``"string"``   | string          | java.lang.``String``
``456``        | number          | java.lang.``Integer``
``2147483648`` | number          | java.lang.``Long``
``12.56``      | number          | java.lang.``Double``
``[]``         | array           | scala.collection.immutable.``List[Any]`` 
               |                 | java.util.``List<Object>``
``{}``         | object          | scala.collection.immutable.``Map[String, Any]``
               |                 | java.util.``Map<String, Object>`§`
``true``       | boolean         | java.lang.``Boolean``
``false``      | boolean         | java.lang.``Boolean``
``null``       | null            | ``null``


### Unparsed
If you want full control of the deserialization process then you'll have to access to the **unparsed** data as follows:

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

