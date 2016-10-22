# Akka Wamp 
[![Build Status][travis-image]][travis-url] [![CodeCov Status][codecov-image]][codecov-url] [![Gitter][gitter-image]][gitter-url] 

     
Akka Wamp is a WAMP - [Web Application Messaging Protocol](http://wamp-proto.org/) implementation written in [Scala](http://scala-lang.org/) with [Akka](http://akka.io/)

Easy to download as [SBT](http://www.scala-sbt.org/) library dependency.

```scala
libraryDependencies ++= Seq(
  "com.github.angiolep" % "akka-wamp_2.11" % "0.11.0"
)  
```

## Client APIs
Connect to a router, open a session, subscribe a topic, receive events, register a procedure and call it in few lines of Scala!

* Actor, Future and Stream based APIs
* Well documented and rich examples
* Lazy and pluggable deserialization

Please, read the docs for [further details](https://angiolep.github.io/projects/akka-wamp/client/overview)

```scala
object PubSubApp extends App {

  import akka.wamp.client._
  val client = Client()

  implicit val ec = client.executionContext

  val publication = 
    for {
      session <- client
        .openSession(
          url = "ws://localhost:8080/ws",
          subprotocol = "wamp.2.json",
          realm = "default.realm",
          roles = Set("subscriber"))
      subscription <- session
        .subscribe(
          topic = "myapp.topic1")(
          event =>
            event.data.map(println)
        )
      publication <- session
        .publish(
          topic = "myapp.topic2",
          ack = false,
          kwdata = Map("name"->"paolo", "age"->40)
        )
    } yield ()
}
```


## Router
 
[![Download][download-image]][download-url]
 
Akka Wamp provides you with a router that can be either embedded into your application or launched as standalone server process.

Download the latest router version, extract, configure and run it as standalone application:

```bash
curl https://dl.bintray.com/angiolep/universal/akka-wamp-0.11.0.tgz
tar xvfz akka-wamp-0.11.0.tar.gz
cd akka-wamp-0.11.0
vim ./conf/application.conf
./bin/akka-wamp -Dakka.loglevel=DEBUG
```

Please, read the docs for [further details](https://angiolep.github.io/projects/akka-wamp/router)



## Limitations

 * Scala 2.11 only (no older Scala and no Java yet)
 * WebSocket transport only (no raw TCP and no SSL/TLS yet) 
 * Provide WAMP Basic Profile only (no Advanced Profile yet)
 * Provide JSON serialization only (no MsgPack yet)

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
