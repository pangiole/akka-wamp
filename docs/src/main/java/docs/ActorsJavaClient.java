package docs;

// #client
import java.net.URI;

import akka.actor.*;
import akka.wamp.*;
import akka.wamp.client.*;
import akka.wamp.messages.*;
import akka.wamp.messages.Error;
import akka.wamp.serialization.*;
import scala.PartialFunction;
import scala.runtime.BoxedUnit;

import static akka.japi.pf.ReceiveBuilder.match;

public class ActorsJavaClient extends AbstractClientLoggingActor {
  // #client

  // #connect
  private ActorRef router;
  private PartialFunction<Object, BoxedUnit> connected;

  // #connect

  
  // #connect
  @Override public void preStart() throws Exception {
    ActorRef manager = Wamp.get(getContext().system()).manager();
    manager.tell(new Connect(new URI("ws://router.net:8080/wamp"), "json"), self());
  }
  
  // #connect

  // #subscribe
  private Long requestId = 0L;
  private Long subscriptionId = 0L;



  // #open
  private PartialFunction<Object, BoxedUnit> open;
  private Long sessionId;
  private SessionScopedIdGenerator gen;

  // #open
  // #subscribe
  private PartialFunction<Object, BoxedUnit> subscribed;
  // #subscribe

  // #client
  // #connect
  // #open
  public ActorsJavaClient() {
    // #subscribe
    receive(
      // #client
      // #open  
      match(CommandFailed.class, sig -> {
        // could reattempt connection ...
      }).
      // #open  
      match(Connected.class, sig -> {
        router = sig.handler();
        context().become(connected);
        // #connect
        router.tell(new Hello("default", Hello.defaultDetails(), validator()), self());
        // #connect
      }).
      build()
      // #client
    );
    // #client
    // #connect

    // #subscribe
    // #publish
    connected =
      // #subscribe
      // #publish
      match(Disconnected.class, sig -> {
        this.sessionId = 0L;
        this.router = null;
        // reattempt connection ...
        // open new sesson ...
        // restore subscriptions/registrations ...
      }).
      match(Abort.class, msg -> {
        this.sessionId = 0L;
        // open new sesson ...
      }).
      // #subscribe  
      // #publish
      match(Welcome.class, msg -> {
        this.sessionId = msg.sessionId();
        this.gen = new SessionScopedIdGenerator();
        context().become(open);
        // submit subscriptions/registrations
        // #publish
        // #open
        this.requestId = gen.nextId();
        router.tell(new Subscribe(requestId, Subscribe.defaultOptions(), "mytopic", validator()), self());
        // #subscribe
        // #publish
        router.tell(new Publish(gen.nextId(), Publish.defaultOptions(), "mytopic", Payload.apply(), validator()), self());
        // TODO tell publish with acknowledgment
        // #publish
        router.tell(new Register(gen.nextId(), Register.defaultOptions(), "myproc", validator()), self());
        // #subscribe
        // #publish
        // #open
      }).
      build();
    
      // #subscribe
      // #publish
      // #open
    
    // #subscribe
    open =
      match(Disconnected.class, sig -> {
        // ...
      }).
      match(Goodbye.class, msg -> {
        this.sessionId = 0L;
        // open new sesson ...
        // restore subscriptions/registrations ...
      }).
      match(Error.class, msg -> {
        if (msg.requestType() == 33 && this.requestId == msg.requestId()) {
          context().stop(self());
        }
      }).
      match(Subscribed.class, msg -> {
         if (this.requestId == msg.requestId()) {
           this.subscriptionId = msg.subscriptionId();
           context().become(subscribed);
           // OR become anyElseYouLike
         }
      }).
      build();
    // #subscribe
    // #open
    // #subscribe
    // #client
    // #connect
  }
  // #connect
  // #client
  // #open
  // #subscribe
  
  // #client
}
// #client

