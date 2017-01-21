
/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.md', which is part of this source code package.
 */

package docs;


// #client
import akka.actor.*;
import akka.wamp.client.japi.*;
import com.typesafe.config.*;

// #client

import akka.event.LoggingAdapter;
import java.util.concurrent.*;
import akka.Done;
import java.util.function.*;
import akka.stream.javadsl.Source;
import akka.util.ByteString;

import java.util.*;
import static java.util.Arrays.asList;



// #client
public class FuturesJavaClient {
  public FuturesJavaClient() {
    Config config = ConfigFactory.load("myapp.conf");
    ActorSystem system = ActorSystem.create("myapp", config);
    Client client = Client.create(system);
    // #client
    
    LoggingAdapter log = system.log();
    
    // #connect
    // import java.util.concurrent.CompletionStage;
    CompletionStage<Connection> conn = client.connect("myrouter");
    // #connect
    
    // #open
    // CompletionStage<Connection> conn =
    CompletionStage<Session> session = conn.thenCompose(c -> c.open("myrealm"));
    // #open

    // ~~~~~~~~~~~~~~~~~~~~~~~~~


    // #publish
    // fire and forget
    session.thenAccept(s -> s.publish("mytopic"));

    // with acknowledge
    CompletionStage<Publication> publication =
      session.thenCompose(s -> s.publishAck("mytopic"));
    // #publish

    // #publication-completion
    publication.whenComplete((pb, ex) -> {
      if (pb != null)
        log.info("Published with {}", pb.id());
      else
        log.error(ex.getMessage(), ex);
    });
    // #publication-completion


    // #event-consumer
    Function<Event, CompletionStage<Done>> consumer =
      event -> {
        Long publicationId = event.publicationId();
        Long subscriptionId = event.subscriptionId();
        Map<String, Object> details = event.details();
        return event.args().thenApply(args -> {

          // so something with arguments ...

          return Done.getInstance();
        });
      };
    // #event-consumer

    // #subscribe
    // Function<Event, CompletionStage<Done>> consumer = ...;
    CompletionStage<Subscription> subscription =
        session.thenCompose(s -> s.subscribe("mytopic", consumer));
    // #subscribe


    // #subscription-completion
    subscription.whenComplete((sb, ex) -> {
      if (sb != null)
        log.info("Subscribed to {} with {}", sb.topic(), sb.id());
      else
        log.error(ex.getMessage(), ex);
    });
    // #subscription-completion


    // #unsubscribe
    CompletionStage<Unsubscribed> unsubscribed =
      subscription.thenCompose(s -> s.unsubscribe());
    // #unsubscribe


    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    // #call
    CompletionStage<Result> result =
      session.thenCompose(s -> s.call("myprocedure", asList("paolo", 99)));
    // #call

    // #result
    result.whenComplete((res, ex) -> {
      if (res != null)
        log.info("Result: {}", res);
      else
        log.error(ex.getMessage(), ex);
    });
    // #result


    // #invocation-handler
    Function<Invocation, CompletionStage<Payload>> handler =
      invocation -> {
        Long registrationId = invocation.registrationId();
        Map<String, Object> details = invocation.details();
        return invocation.args().thenApply(args -> {

          // do something with arguments ...

          Object res = null;
          return Payload.create(asList(res));
        });
      };
    // #invocation-handler

    // #register
    // Function<Invocation, CompletionStage<Payload>> handler = ...
    CompletionStage<Registration> registration =
      session.thenCompose(s -> s.register("myprocedure", handler));
    // #register

    // #registration
    registration.whenComplete((reg, ex) -> {
      if (reg != null)
        log.info("Registered with {}", reg.id());
      else
        log.error(ex.getMessage(), ex);
    });
    // #registration

    // #unregister
    CompletionStage<Unregistered> unregistered =
      registration.thenCompose(r -> r.unregister());
    // #unregister

    Event conveyor = null;
    // #incoming-payload
    // Event conveyor = ...
    if (conveyor.payload() instanceof TextLazyPayload) {
      TextLazyPayload p = (TextLazyPayload) conveyor.payload();
      Source<String, ?> unparsed = p.unparsed();
      // parse textual source ...
    }
    else if (conveyor.payload() instanceof BinaryLazyPayload) {
      BinaryLazyPayload p = (BinaryLazyPayload) conveyor.payload();
      Source<ByteString, ?> unparsed = p.unparsed();
      // parse binary source ...
    }
    // #incoming-payload

    // #incoming-arguments
    // Payload conveyors are messages such as events, invocations, errors, etc.
    CompletionStage<List<Object>> args = conveyor.args();
    CompletionStage<Map<String, Object>> kwargs = conveyor.kwargs();
    CompletionStage<UserType> user = conveyor.kwargs(UserType.class);
    // #incoming-arguments

    // #outgoing-arguments
    // empty payload
    Payload empty = Payload.create();

    // list of indexed arguments
    Payload indexed = Payload.create(asList("paolo", 99, true));

    // dictionary of named arguments
    Payload named = Payload.create(new HashMap<String, Object>(){{
      put("name", "paolo");
      put("age", 99);
      put("male", true);
    }});
    // #outgoing-arguments
    

    
    // #all-together
    // TBD
    // #all-together

    // #close
    CompletionStage<Closed> closed = session.thenCompose(s -> s.close());
    // #close

    // #disconnect
    CompletionStage<Disconnected> disconnected = conn.thenCompose(c -> c.disconnect());
    // #disconnect

    // #client
    // ...
  }
  // #client
  
  // #incoming-arguments
  
    public class UserType { public String name; public Integer age; }
  // #incoming-arguments

  // #client
}
// #client
