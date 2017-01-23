/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.md', which is part of this source code package.
 */

/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.md', which is part of this source code package.
 */

import akka.actor.ActorSystem;
import akka.wamp.client.japi.*;
import com.typesafe.config.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.util.Arrays.asList;
import static java.lang.System.out;

public class FuturesJavaClient {
  public static void main(String[] arr) {

    Config config = ConfigFactory.load("my.conf");
    ActorSystem system = ActorSystem.create("myapp", config);
    Client client = Client.create(system);

    client.connect("local").thenAccept(c -> {
      c.open("realm").thenAccept(s -> {

        s.publish("topic", asList("Ciao!"));

        s.subscribe("topic", (event) -> {
          out.println("got " + event.args().get(0));
        });

        s.register("procedure", (invocation) -> {
          List<Object> args = invocation.args();
          Integer a = (Integer) args.get(0);
          Integer b = (Integer) args.get(1);
          Integer result = a + b;
          // TODO Simplify Java API
          return CompletableFuture.completedFuture(Payload.create(asList(result)));
        });

        s.call("procedure", asList(20, 55)).thenAccept(res -> {
          out.println("20 * 55 = " + res.args().get(0));
        });
      });
    });
  }
}


