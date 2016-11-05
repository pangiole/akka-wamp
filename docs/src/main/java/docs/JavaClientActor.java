package docs;

// #client
import akka.actor.*;
import akka.wamp.*;
import akka.wamp.client.japi.AbstractClientLoggingActor;
import akka.wamp.messages.*;
import akka.wamp.messages.Error;
import akka.wamp.serialization.Payload;
import scala.PartialFunction;
import scala.runtime.BoxedUnit;

import static akka.japi.pf.ReceiveBuilder.match;

public class JavaClientActor extends AbstractClientLoggingActor {
  private ActorRef conn;
  private Long sessionId;
  private PartialFunction<Object, BoxedUnit> connected, open, subscribed;
  
  // NOTE: receive pattern matches are defined within constructor
  // #client
  
  // #connect
  @Override public void preStart() throws Exception {
    ActorRef manager = Wamp.get(getContext().system()).manager();
    manager.tell(new Connect("ws://router.net:8080/wamp", "json"), self());
  }
  
  // #connect

  // #subscribe
  private Long requestId = 0L;
  private Long subscriptionId = 0L;

  // #connect
  // #client
  // #open
  public JavaClientActor() {
    // #subscribe
    receive(
      // #client
      // #open  
      match(CommandFailed.class, sig -> {
        // reattempt connection ...
      }).
      // #open  
      match(Connected.class, sig -> {
        conn = sig.handler();
        context().become(connected);
        // #connect
        conn.tell(new Hello("default", Hello.defaultDetails(), validator()), self());
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
        this.conn = null;
        // reattempt connection ...
        // reopen session ...
        // restore subscriptions/registrations ...
      }).
      match(Abort.class, msg -> {
        this.sessionId = 0L;
        log().warning(msg.reason());
        context().stop(self());
      }).
      // #subscribe  
      // #publish
      match(Welcome.class, msg -> {
        this.sessionId = msg.sessionId();
        context().become(open);
        // submit subscriptions/registrations
        // #publish
        // #open
        this.requestId = nextRequestId();
        conn.tell(new Subscribe(requestId, Subscribe.defaultOptions(), "myapp.topic", validator()), self());
        // #subscribe
        // #publish
        conn.tell(new Publish(nextRequestId(), Publish.defaultOptions(), "myapp.topic", Payload.apply(), validator()), self());
        // TODO tell publish with acknowledgment
        // #publish
        conn.tell(new Register(nextRequestId(), Register.defaultOptions(), "myapp.procedure", validator()), self());
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
        log().warning(msg.reason());
        // reopen session ...
        // restore subscriptions/registrations ...
      }).
      match(Error.class, msg -> {
        if (msg.requestType() == 33 && this.requestId == msg.requestId()) {
          log().warning(msg.error());
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

