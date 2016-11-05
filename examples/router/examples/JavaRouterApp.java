package examples;

import akka.actor.*;
import akka.wamp.router.*;

public class JavaRouterApp {
  public static void main(String[] args) {
    ActorSystem system = ActorSystem.create();
    EmbeddedRouter.createAndBind(system);
    // ...
  }
}
