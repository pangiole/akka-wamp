/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.md', which is part of this source code package.
 */

import akka.Done;
import akka.actor.ActorSystem;
import akka.wamp.client.japi.*;
import com.typesafe.config.*;

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
          return event.args().thenApply(args -> {
            out.println("got " + args.get(0));
            return Done.getInstance();
          });
        });

        s.register("procedure", (invocation) -> {
          return invocation.args().thenApply(args -> {
            Integer a = (Integer) args.get(0);
            Integer b = (Integer) args.get(1);
            Integer result = a + b;
            return Payload.create(asList(result));
          });
        });

        s.call("procedure", asList(20, 55)).thenAccept(res -> {
          res.args().thenAccept(args -> {
            out.println("20 * 55 = " + args.get(0));
          });
        });
      });
    });
  }
}


