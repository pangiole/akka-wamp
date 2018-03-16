package akka.wamp

/**
  * Contains classes, traits, types and functions to be used to implement WAMP clients in Scala.
  *
  * Please refer to the official
  * <a href="https://angiolep.github.io/projects/akka-wamp/client/index.html">Akka Wamp User's Guide</a>
  * for further details.
  *
  *
  * == Actors ==
  *
  * Is the low level Client API.
  *
  *
  * == Futures ==
  *
  * Is the high level Client API we encourage you to use.
  *
  * {{{
  * import akka.actor._
  * import akka.wamp.client._
  * import com.typesafe.config._
  *
  * val config = ConfigFactory.load("my.conf")
  * val system = ActorSystem("myapp", config)
  * val client = Client(system)
  *
  * client.connect("myrouter").foreach { conn =>
  *   conn.open("myrealm").foreach { implicit s =>
  *
  *     publish("mytopic", List("paolo", 99))
  *
  *     subscribe("mytopic", (name: String, age: Int) = {
  *       println(s"$name is $age years old")
  *     }
  *
  *     call("myprocedure", List("paolo", 99))
  *
  *     register("myprocedure", (name: String, age: Int) => {
  *       name.length + age
  *     })
  *   }
  * }
  * }}}
  *
  * == Streams ==
  *
  * Working in progress.
  *
  *
  * @see [[akka.wamp.client.japi]] for Java API
  */
package object client