import akka.actor.*;
import akka.wamp.client.japi.*;

public class PoloniexJavaClient {
  public static void main(String[] aaa) {
    ActorSystem actorSystem = ActorSystem.create();
    Client client = Client.create(actorSystem);

    client.connect("wss://api.poloniex.com", "json").thenAccept(conn -> {
      conn.open("realm1").thenAccept(session -> {

        session.subscribe("ticker", event -> {
          System.out.printf("%s --> %s\n", event.kwargs(), event.args());
        });
      });
    });
  }
}
