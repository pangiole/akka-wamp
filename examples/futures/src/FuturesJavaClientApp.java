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

import static java.util.Arrays.asList;
import static java.lang.System.out;
import static java.lang.System.exit;

public class FuturesJavaClientApp {
  public static void main(String[] arr) {

    String endpoint = null;
    if (arr.length > 0) endpoint = arr[0];
    else endpoint = "default";

    Config config = ConfigFactory.load("application.conf");
    ActorSystem system = ActorSystem.create("myapp", config);
    Client client = Client.create(system);

    client.connect(endpoint).thenAccept(conn -> {
      conn.open("default").thenAccept(sess -> {

        sess.publish("topic", asList("Ciao!"));

        sess.subscribe("topic", (event) -> {
          out.println("got " + event.args().get(0));
        });

        sess.register("procedure", (invocation) -> {
          List<Object> args = invocation.args();
          Integer a = (Integer) args.get(0);
          Integer b = (Integer) args.get(1);
          Integer result = a + b;
          return result;
        });

        sess.call("procedure", asList(20, 55)).thenAccept(res -> {
          out.println("20 + 55 = " + res.args().get(0));

          // Finally WAMP disconnect, terminate Akka and exit the JVM
          conn.disconnect();
          system.getWhenTerminated().thenAccept(t -> {
            exit(0);
          });
          system.terminate();
        });
      });
    });
  }
}


