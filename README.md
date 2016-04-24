# akka-wamp
WAMP - Web Application Messaging Protocol implementation written in Scala with Akka HTTP.

[![Build Status][travis-image]][travis-url] [![Codacy Status][codacy-image]][codacy-url]


## Router
It provides a Router that can be easily embedded into your application or launched as standalone.

### Embedded
Make your project depend on akka-wamp:
```scala
//file: build.sbt

libraryDependencies += "angiolep.github.com" %% "akka-wamp" % "0.1.0-SNAPSHOT"
```

Instantiate and start the Router:
```scala
//file: src/main/scala/myapp/myClass.scala

import akka.wamp.transports.WebSocketRouter

val router = new WebSocketRouter()
// start the router asynchronously
router.start()

// ... your code follows ...
```

### Standalone
Download and launch the Router as standalone application:

```bash
curl http://angiolep.github.io/akka-wamp-0.1.0.tar.gz
tar xvfz akka-wamp-0.1.0.tar.gz
cd akka-wamp-0.1.0
./bin/akka-wamp -Dakka.wamp.port=7070
```

### Limitations
DO NOT use this software in production as it does NOT provide a complete WAMP implementation yet but only the following features:

 * Router with _broker_ role able to handle sessions, subscriptions and publications.
 * WAMP Basic Profile only!
 
No _dealer_ role (hence no procedures registrations and invocations) and no WAMP Advanced Profile features are provided yet.
 
## Client
Not provided yet. Please use some other [WAMP Libraries](http://wamp-proto.org/implementations/).


[travis-image]: https://travis-ci.org/angiolep/akka-wamp.svg?branch=master
[travis-url]: https://travis-ci.org/angiolep/akka-wamp

[codacy-image]: (https://api.codacy.com/project/badge/grade/f66d939188b944bbbfacde051a015ca1)]
[codacy-url]: (https://www.codacy.com/app/paolo-angioletti/akka-wamp)