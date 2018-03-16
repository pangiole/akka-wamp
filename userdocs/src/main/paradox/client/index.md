@@@ index
* [Config](config.md)
* [Actors](actors.md)
* [Futures](futures.md)
* [Streams](streams.md)
* [Macros](macros.md)
@@@ 

# Client APIs
Akka Wamp provides your with three Client APIs designed for both the Java and Scala programming languages. The APIs are all built atop the same [Akka I/O](http:/doc.akka.io/docs/akka/current/scala/io.html) extension driver but they have been distinguished to be used with different Akka abstractions for concurrency such as actors, futures and streams.

* [Futures](futures.html)   
  High level API, simple to use, meant for everyone to enjoy. 
  
* [Actors](actors.html)   
  Low level API meant to be used by protocol implementors. It requires deep understanding of the WAMP protocol, its messages and its exchange rules.
  
* [Streams](streams.html)  
  Not yet supported.
  
* [Macros](macros.html)  
  For Scala developers only, some utilities meant to reduce _"boiler plate"_ and make the source code look nice!
   
   
@@@ note
Futures based Client API is the best option if don't want to deal with internals of Akka Wamp or the specification of the WAMP protocol.
@@@