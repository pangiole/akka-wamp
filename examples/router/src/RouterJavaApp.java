import akka.actor.ActorSystem;
import akka.wamp.router.EmbeddedRouter;

public class RouterJavaApp {
  public static void main(String[] args) {

    // Either the actor system or an actor context
    ActorSystem actorFactory = ActorSystem.create();

    // Just create and bind the router actor as per its configuration
    EmbeddedRouter.createAndBind(actorFactory);

    // ...
  }
}
