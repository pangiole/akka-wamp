
import akka.Done;
import akka.actor.ActorSystem;
import akka.wamp.client.japi.Client;

public class PoloniexJavaClient {
  public static void main(String[] aaa) {
    ActorSystem actorSystem = ActorSystem.create();
    Client client = Client.create(actorSystem);
    client.connect("wss://api.poloniex.com", "json").thenAccept(c -> {
      c.open("realm1").thenAccept(s -> {
        s.subscribe("ticker", event -> {
          return event.args().thenApply(args -> {
              System.out.println(args.toString());
              return Done.getInstance();
            });
        });
      });
    });
  }
}
