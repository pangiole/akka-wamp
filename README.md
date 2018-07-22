# Akka Wamp 
[![Build Status][travis-image]][travis-url] [![CodeCov Status][codecov-image]][codecov-url] [![Gitter][gitter-image]][gitter-url] 

Akka Wamp is a WAMP - [Web Application Messaging Protocol](http://wamp-proto.org/) implementation written to let both [Scala](http://scala-lang.org/) and [Java](http://www.java.com) developers build the next generation of reactive web services on top of [Akka](http://akka.io/) abstractions.

Akka Wamp provides you with:

* Simple [Client APIs](https://angiolep.github.io/projects/akka-wamp/client) designed to be used with [Akka](http://akka.io/) actors, futures and streams.
* Object-oriented representations of all WAMP [Messages](./messages.html),
* Akka IO extenson driver for the WAMP Protocol.
* Basic [Router](https://angiolep.github.io/projects/akka-wamp/router) you can embed in your applications or launch as standalone process.

## Usage
Easy to download as dependency from [Maven central](https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.github.angiolep%22%20AND%20a%3A%22akka-wamp_2.12%22):

```scala
libraryDependencies ++= Seq(
  "com.github.angiolep" % "akka-wamp_2.12" % "0.15.1"
)
```

## Docs
* User's guide, code fragments and dozens of examples are published [here](https://angiolep.github.io/projects/akka-wamp).
* API doc is published [here](https://angiolep.github.io/projects/akka-wamp/api/akka/wamp)


## Client APIs
Connect to a router, open a session, subscribe to a topic, consume events, register a remote procedure and call it in few lines of Scala or Java code.

* Actors, Futures and Streams based APIs.
* Lambda consumers and handlers support.
* Lazy and pluggable deserializers.
* Java 8 support.
* ... and much more!

Please, read the docs for [further details](https://angiolep.github.io/projects/akka-wamp)


### Java client
Though written in Scala, Akka Wamp provides simple client APIs for Java developers as well. Compared to other WAMP implementations, Akka Wamp will let you write less code for much more functionalities!

```java
import akka.actor.*;
import akka.wamp.client.japi.*;

import static java.util.Array.asList;
import static java.lang.System.out;

public class JavaClient {
  public static void main(String[] arr) {
    ActorSystem actorSystem = ActorSystem.create();
    Client client = Client.create(actorSystem);
    
    client.connect("endpoint").thenAccept(c -> {
      c.open("realm").thenAccept(s -> {
    
        s.publish("topic", asList("Ciao!"));
    
        s.subscribe("topic", (event) -> {
          out.println("got " + event.args().get(0));
        });
    
        s.register("procedure", (invoc) -> {
          Integer a = (Integer) invoc.args().get(0);
          Integer b = (Integer) invoc.args().get(1);
          return a + b;
        });
    
        s.call("procedure", asList(20, 55)).thenAccept(res -> {
          out.println("20 * 55 = " + res.args().get(0));  
        });
      });
    });
  }
}
```


### Scala client
Akka Wamp provides Scala developer with great support to let them write _"no boilerplate code"_ at all! Just few statements and here it is a fully capable reactive WAMP client ;-)

```scala

import akka.actor._
import akka.wamp.client._

object ScalaClient extends App {
  
  val system = ActorSystem()
  val client = Client(system)
  implicit val executionContext = system.dispatcher

  client.connect("endpoint").map { conn =>
    conn.open("realm").map { implicit session =>

      subscribe("topic", (arg: Int) => {
        println(s"got $arg")
      })

      publish("topic", List("Ciao!"))

      call("procedure", List(20, 55)).foreach { res =>
        println(s"20 * 55 = ${res.args(0)}")
      }
      
      register("procedure", (a: Int, b: Int) => {
        a + b
      })
    }
  }
}
```

## Router
 
[![Download][download-image]][download-url]
 
Akka Wamp provides you with a basic router that can be either embedded into your application or launched as standalone server process.

Download the latest router version, extract, configure and run it as standalone application:

```bash
curl https://dl.bintray.com/angiolep/universal/akka-wamp-0.15.2.tgz
tar xvfz akka-wamp-0.15.2.tar.gz
cd akka-wamp-0.15.2
vim ./conf/application.conf
./bin/akka-wamp -Dakka.loglevel=DEBUG
```


## Limitations
 * Java >= 1.8.0 
 * Scala >= 2.12.0
 * Akka >= 2.5.0
 * WebSocket transport only (no raw TCP) 
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
[download-url]: https://bintray.com/angiolep/universal/download_file?file_path=akka-wamp-0.15.2.tgz

