@@@ index
* [Router](router/index.md)
* [Client](client/index.md)
* [Messages](messages.md)
* [Payloads](payloads.md)
* [Logging](logging.md)
* [TLS](tls.md)

@@@


# Akka Wamp
[![Build Status][travis-image]][travis-url] [![CodeCov Status][codecov-image]][codecov-url] [![Gitter][gitter-image]][gitter-url] 

Akka Wamp is a WAMP - [Web Application Messaging Protocol](http://wamp-proto.org/) implementation written to let both [Scala](http://scala-lang.org/) and [Java](http://www.java.com) developers build the next generation of reactive web services on top of [Akka](http://akka.io/) abstractions.

Akka Wamp provides you with:

* Simple [Client APIs](https://angiolep.github.io/projects/akka-wamp/client) designed to be used with [Akka](http://akka.io/) actors, futures and streams.
* Object-oriented representations of all WAMP [Messages](./messages.html),
* Akka IO extenson driver for the WAMP Protocol.
* Basic [Router](https://angiolep.github.io/projects/akka-wamp/router) you can embed in your applications or launch as standalone process.

## Usage
Depending on which kind of application you wish to implement, make your Java or Scala build depend on the latest version of either ``akka-wamp-client`` or ``akka-wamp-router`` or both:
 
sbt
:   @@snip [build.sbt](./build.sbt)

mvn
:    @@snip [pom.xml](./pom.xml)

gradle
:    @@snip [build.gradle](./build.gradle)


## Limitations
 * Java >= 1.8.0 
 * Scala >= 2.12.0
 * Akka >= 2.5.0
 * WebSocket transport only (no raw TCP) 
 * WAMP Basic Profile only (none of the Advanced Profile features yet)
 * JSON serialization only (no MsgPack yet)
 * Not yet ready for production


## Disclaimer
> This SOFTWARE PRODUCT is provided by THE PROVIDER "as is" and "with all faults." THE PROVIDER makes no representations or warranties of any kind concerning the safety, suitability, lack of viruses, inaccuracies, typographical errors, or other harmful components of this SOFTWARE PRODUCT. There are inherent dangers in the use of any software, and you are solely responsible for determining whether this SOFTWARE PRODUCT is compatible with your equipment and other software installed on your equipment. You are also solely responsible for the protection of your equipment and backup of your data, and THE PROVIDER will not be liable for any damages you may suffer in connection with using, modifying, or distributing this SOFTWARE PRODUCT

[travis-image]: https://travis-ci.org/angiolep/akka-wamp.svg?branch=master
[travis-url]: https://travis-ci.org/angiolep/akka-wamp

[codecov-image]: https://codecov.io/gh/angiolep/akka-wamp/branch/master/graph/badge.svg
[codecov-url]: https://codecov.io/gh/angiolep/akka-wamp
        
[gitter-image]: https://badges.gitter.im/angiolep/akka-wamp.svg
[gitter-url]: https://gitter.im/angiolep/akka-wamp?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=body_badge

[download-image]: https://api.bintray.com/packages/angiolep/universal/akka-wamp/images/download.svg
[download-url]: https://bintray.com/angiolep/universal/akka-wamp/_latestVersion
