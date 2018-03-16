/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.md', which is part of this source code package.
 */

package akka.wamp.client

/**
  *
  * Contains classes, traits, types and functions to be used to implement WAMP clients in Java.
  *
  * Please refer to the official
  * <a href="https://angiolep.github.io/projects/akka-wamp/index.html">Akka Wamp User's Guide</a>
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
  * import akka.actor.*;
  * import akka.wamp.client.japi.*;
  * import static java.util.Array.asList;
  * import static java.lang.System.out;
  *
  * // ...
  *
  * ActorSystem actorSystem = ActorSystem.create();
  * Client client = Client.create(actorSystem);
  *
  * client.connect("endpoint").thenAccept(c -> {
  *   c.open("realm").thenAccept(s -> {
  *
  *      s.publish("topic", asList("Ciao!"));
  *
  *      s.subscribe("topic", (event) -> {
  *        out.println("got " + event.args().get(0));
  *      });
  *
  *      s.register("procedure", (invoc) -> {
  *        Integer a = (Integer) invoc.args().get(0);
  *        Integer b = (Integer) invoc.args().get(1);
  *        return a + b;
  *      });
  *
  *      s.call("procedure", asList(20, 55)).thenAccept(res -> {
  *        out.println("20 * 55 = " + res.args().get(0));
  *      });
  *   });
  * });
  * }}}
  *
  * == Streams ==
  *
  * Working in progress.
  *
  *
  * @note Java API
  * @see [[akka.wamp.client]]
  */
package object japi {

}
