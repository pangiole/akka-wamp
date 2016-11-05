# Akka Wamp 
[![Build Status][travis-image]][travis-url] [![CodeCov Status][codecov-image]][codecov-url] [![Gitter][gitter-image]][gitter-url] 

Akka Wamp is a WAMP - [Web Application Messaging Protocol](http://wamp-proto.org/) implementation written to let both [Scala](http://scala-lang.org/) and [Java](http://www.java.com) developers build the next generation of reactive web services on top of [Akka](http://akka.io/) abstractions.

Akka Wamp provides you with:

* Simple [Client APIs](client/index.html) designed to be used with [Akka](http://akka.io/) actors, futures and streams.
* Object-oriented representations of all WAMP [Messages](./messages.html),
* Akka IO extenson driver for the [WAMP Protocol](https://tools.ietf.org/html/draft-oberstet-hybi-tavendo-wamp-02).
* Basic [Router](router/index.html) you can embed in your applications or launch as standalone process.

## Usage
Easy to download as dependency from Maven central:

```scala
"com.github.angiolep" %% "akka-wamp" % "0.13.0"
```

## Docs
* User's guide, code fragments and dozens of examples are published [here](https://angiolep.github.io/projects/akka-wamp/index.html).
* API doc is published [here](https://angiolep.github.io/projects/akka-wamp/api/akka/wamp/index.html)
)

## Client APIs
Connect to a router, open a session, subscribe a topic, receive events, register a procedure and call it in few lines of Scala or Java code.

* Actors, Futures and Streams based APIs.
* Lambda handlers support.
* Lazy and pluggable deserializers.
* Java 8 support.
* ... and much more!

Please, read the docs for [further details](https://angiolep.github.io/projects/akka-wamp/client/index.html)


### Java client
Though written in Scala, Akka Wamp provides simple client APIs for Java developers as well. Compared to others, Akka Wamp will let you write less code for much more functionalities!

```java
import akka.actor.ActorSystem;
import akka.wamp.client.japi.*;

import static java.util.Array.asList;
import static java.lang.System.out;

public class JavaClient {
  public static void main(String[] arr) {

    ActorSystem system = ActorSystem.create();
    Client client = Client.create(system);
    client.connect("default").thenAccept(c -> {
      c.open("myrealm").thenAccept(s -> {
    
        s.publish("mytopic", asList("Ciao!"));
    
        s.subscribe("mytopic", (event) -> {
          event.args().thenAccept(args -> {
            out.println("got " + args.get(0));
          });
        });
    
        s.register("myproc", (invoc) -> {
          return invoc.args().thenApply(args -> {
            Integer a = (Integer) args.get(0);
            Integer b = (Integer) args.get(1);
            Integer result = a + b;
            return Payload.create(asList(result));
          });
        });
    
        s.call("myproc", asList(20, 55)).thenAccept(res -> {
          res.args().thenAccept(args -> {
            out.println("20 * 55 = " + args.get(0));  
          });
        });
      });
    });
  }
}
```

Please, read the docs for [further details](https://angiolep.github.io/projects/akka-wamp/client/index.html)


### Scala client
Akka Wamp provides Scala developer with great support to let them write _"no boilerplate code"_ at all! Just few statements and here it is a fully capable reactive WAMP client ;-)

```scala
// ... 
```

Please, read the docs for [further details](https://angiolep.github.io/projects/akka-wamp/client/index.html)


## Router
 
[![Download][download-image]][download-url]
 
Akka Wamp provides you with a basic router that can be either embedded into your application or launched as standalone server process.

Download the latest router version, extract, configure and run it as standalone application:

```bash
curl https://dl.bintray.com/angiolep/universal/akka-wamp-0.13.0.tgz
tar xvfz akka-wamp-0.13.0.tar.gz
cd akka-wamp-0.13.0
vim ./conf/application.conf
./bin/akka-wamp -Dakka.loglevel=DEBUG
```

Please, read the docs for [further details](https://angiolep.github.io/projects/akka-wamp/router.html)


## Limitations
 * Java >= 1.8.0 
 * Scala = 2.11
 * WebSocket transport only (no raw TCP and no SSL/TLS yet) 
 * WAMP Basic Profile only (none of the Advanced Profile features yet)
 * JSON serialization only (no MsgPack yet)
 * Not yet ready for production

## Changelog
Please, read [CHANGELOG.md](CHANGELOG.md)

## Contributing
Please, read [CONTRIBUTING.md](CONTRIBUTING.md)

## Licence 
This software comes with [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)

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
