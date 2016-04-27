# akka-wamp
WAMP - Web Application Messaging Protocol implementation written in Scala with Akka HTTP.

[![Build Status][travis-image]][travis-url] [![Codacy Status][codacy-image]][codacy-url]


## Router
It provides a Router that can be easily embedded into your application or launched as standalone.

### Embedded
Make your SBT build depend on akka-wamp:
```scala
scalaVersion := "2.11.7"

libraryDependencies += "com.github.angiolep" %% "akka-wamp" % "0.1.0"
```

Instantiate and start the Router (with WebSocket transport) in any of your classes:
```scala
import akka.wamp.WebSocketRouter
new WebSocketRouter().start()
```

### Standalone
Download and launch the Router as standalone application:

```bash
curl http://angiolep.github.io/akka-wamp-0.1.0.tar.gz
tar xvfz akka-wamp-0.1.0.tar.gz
cd akka-wamp-0.1.0
./bin/akka-wamp -Dakka.loglevel=DEBUG -Dakka.wamp.port=7070
```

### Limitations
DO NOT use this software in production as it does NOT provide a complete implementation yet.

 * It works with Scala 2.11 only.
 * It provides WebSocket transport only.
 * The WebSocketRouter works as _broker_ only (_dealer_ is NOT provided yet).
 * The WebSocketClient is not provided yet.
 * It implements the WAMP Basic Profile only.
 
 
## Client
Not provided yet. Please use some other [WAMP Libraries](http://wamp-proto.org/implementations/).


[travis-image]: https://travis-ci.org/angiolep/akka-wamp.svg?branch=master
[travis-url]: https://travis-ci.org/angiolep/akka-wamp

[codacy-image]: https://api.codacy.com/project/badge/grade/f66d939188b944bbbfacde051a015ca1
[codacy-url]: https://www.codacy.com/app/paolo-angioletti/akka-wamp
