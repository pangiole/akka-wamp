@@@ index
* [Router](router.md)
* [Client APIs](client/index.md)
* [Messages](messages.md)
* [Payloads](payloads.md)
* [Logging](logging.md)

@@@


# Akka Wamp
Akka Wamp is a WAMP - [Web Application Messaging Protocol](http://wamp-proto.org/) implementation written for both [Scala](http://scala-lang.org/) and [Java](http://www.java.com) developers that wish to use [Akka](http://akka.io/) abstractions to write concurrent and reactive applications that are easy to reason about.

It provides you with

* object-oriented representations of WAMP [messages](./messages.html),
* intuitive [Client APIs](client/index.html) designed to be used with actors, futures and streams,
* basic [Router](router/index.html) you can embed in your server application or launch as standalone.


## Usage
Make your Scala or Java build depend on the latest version of akka-wamp:
 
sbt
:   @@snip [build.sbt](./build.sbt)

mvn
:    @@snip [pom.xml](./pom.xml)

gradle
:    @@snip [pom.xml](./build.gradle)


Then read on [Client APIs](client/index.html) for further details.

## Requirements
* Java >= 1.8 
* Scala >= 2.11


## Limitations
* WebSocket transport only (no raw TCP and no SSL/TLS yet) 
* WAMP Basic Profile only (no Advanced Profile yet)
* JSON serialization only (no MsgPack yet)


