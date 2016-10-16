# Akka Wamp 
[![Build Status][travis-image]][travis-url] [![Codacy Status][codacy-image]][codacy-url] [![Gitter][gitter-image]][gitter-url] 

     
Akka Wamp is a WAMP - [Web Application Messaging Protocol](http://wamp-proto.org/) implementation written in [Scala](http://scala-lang.org/) with [Akka](http://akka.io/)

Easy to download as [SBT](http://www.scala-sbt.org/) library dependency.

```scala
libraryDependencies ++= Seq(
  "com.github.angiolep" % "akka-wamp_2.11" % "0.11.0"
)  
```

## Client API
Connect to a router, open a session, subscribe a topic, receive events, register a procedure and call it in few lines of Scala!

```scala
object PubSubApp extends App {

  import akka.wamp.client._
  val client = Client()

  implicit val ec = client.executionContext

  val publication = 
    for {
      session <- client
        .openSession(
          url = "ws://localhost:8080/router",
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


### Major features

* Fully compliant with Reactive Manifesto
* Simple and well documented APIs
* Lazy deserialization

Please, read the docs for [further details](https://angiolep.github.io/projects/akka-wamp/client/overview)


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
> THIS SOFTWARE AVAILABLE ON THE SITE OPENSHA.ORG IS PROVIDED "AS IS" AND ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL OPENSHA.ORG, THE UNIVERSITY OF SOUTHERN CALIFORNIA (USC), OR ANY OF THEIR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

[travis-image]: https://travis-ci.org/angiolep/akka-wamp.svg?branch=master
[travis-url]: https://travis-ci.org/angiolep/akka-wamp

[codacy-image]: https://api.codacy.com/project/badge/grade/f66d939188b944bbbfacde051a015ca1
[codacy-url]: https://www.codacy.com/app/paolo-angioletti/akka-wamp
                
[gitter-image]: https://badges.gitter.im/angiolep/akka-wamp.svg
[gitter-url]: https://gitter.im/angiolep/akka-wamp?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=body_badge

[download-image]: https://api.bintray.com/packages/angiolep/universal/akka-wamp/images/download.svg
[download-url]: https://bintray.com/angiolep/universal/akka-wamp/_latestVersion
