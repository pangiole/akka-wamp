package examples;

import akka.Done;
import akka.actor.ActorSystem;
import akka.wamp.client.japi.*;
import com.typesafe.config.*;

import static java.util.Arrays.asList;
import static java.lang.System.out;

public class JavaFuturesApp {
  public static void main(String[] arr) {

    Config config = ConfigFactory.load("my.conf");
    ActorSystem system = ActorSystem.create("myapp", config);
    Client client = Client.create(system);

    client.connect("default").thenAccept(c -> {
      c.open("myrealm").thenAccept(s -> {

        s.publish("mytopic", asList("Ciao!"));

        s.subscribe("mytopic", (event) -> {
          return event.args().thenApply(args -> {
            out.println("got " + args.get(0));
            return Done.getInstance();
          });
        });

        s.register("myproc", (invocation) -> {
          return invocation.args().thenApply(args -> {
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


