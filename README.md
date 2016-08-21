[![Build Status][travis-image]][travis-url] [![Codacy Status][codacy-image]][codacy-url] [![Documentation Status][docs-image]][docs-url]
     
Akka Wamp is a WAMP - [Web Application Messaging Protocol](http://wamp-proto.org/) implementation written in [Scala](http://scala-lang.org/) with [Akka](http://akka.io/)

## Read the Docs
Detailed documentation is published [here](http://akka-wamp.readthedocs.io/)

## Client APIs
Akka Wamp provides you with three alternative APIs in writing clients:

 * Future based
 * Actor based
 * Stream based

Here it is a code snippet with what you could do with Akka Wamp:
 
```scala
for (session <- Client().connectAndHello("ws://host:8080/ws"))
  yield session.subscribe("myapp.topic") { event =>
    event.payload.map { p =>
      system.log.info(payload.arguments.toString)
    }
  }
```
 
## Router
Akka Wamp provides you with a router that can be either embedded into your application or launched as standalone server process.

## Limitations

 * It works with Scala 2.11 (no older Scala)
 * WebSocket transport without SSL/TLS encryption (no raw TCP yet)  
 * Router works as _broker_ (no _dealer_ yet).
 * Client works as _publisher_/_subscriber_ (no _callee_/_caller_ yet).
 * Provide WAMP Basic Profile (no Advanced Profile yet)
 * Provide JSON serialization (no MsgPack yet)

[travis-image]: https://travis-ci.org/angiolep/akka-wamp.svg?branch=master
[travis-url]: https://travis-ci.org/angiolep/akka-wamp

[codacy-image]: https://api.codacy.com/project/badge/grade/f66d939188b944bbbfacde051a015ca1
[codacy-url]: https://www.codacy.com/app/paolo-angioletti/akka-wamp

[docs-image]: https://readthedocs.org/projects/akka-wamp/badge/?version=latest
[docs-url]: http://akka-wamp.readthedocs.io/en/latest/?badge=latest
