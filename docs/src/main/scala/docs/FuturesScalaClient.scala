package docs


// #client
import akka.actor._
import akka.wamp.client._
import com.typesafe.config._

// #client

import akka.Done
import akka.stream.scaladsl._
import akka.util._
import akka.wamp.messages._
import akka.wamp.serialization._
import scala.util._


// #client
class FuturesScalaClient {

  val config = ConfigFactory.load("myapp.conf")
  val system = ActorSystem("myapp", config)
  val client = Client(system)
  // #client
  
  implicit val executionContext = system.dispatcher
  val log = system.log

  // #connect
  import scala.concurrent.Future
  val conn: Future[Connection] = client.connect("myrouter")
  // #connect
  
  // #open
  // val conn: Future[Connection] = ...
  val session: Future[Session] = conn.flatMap(c => c.open("myrealm"))
  // #open

  // ~~~~~~~~~~~~~~~~~~~~~~~~~~~

  // #publish
  // fire and forget
  session.map(s => s.publish("mytopic"))

  // with acknowledge
  val publication: Future[Publication] =
    session.flatMap(s => s.publishAck("mytopic"))
  // #publish

  // #publication-completion
  publication.onComplete {
    case Success(pb) =>
      log.info(s"Published with ${pb.id}")
    case Failure(ex) =>
      log.error(ex.getMessage, ex)
  }
  // #publication-completion


  {
    val consumer: (Event => Future[Done]) = ???
    // #subscribe
    // val consumer: (Event) => Future[Done]  = ...
    val subscription: Future[Subscription] =
      session.flatMap(s => s.subscribe("mytopic", consumer))
    // #subscribe
  }

  // #subscription-completion
  subscription.onComplete {
    case Success(sb) =>
      log.info(s"Subscribed to ${sb.topic} with ${sb.id}")
    case Failure(ex) =>
      log.error(ex.getMessage, ex)
  }
  // #subscription-completion

  {
  // #event-consumer
  val consumer: (Event) => Future[Done] =
    event => {
      val publicationId = event.publicationId
      val subscriptionId = event.subscriptionId
      val details = event.details
      event.args.map { args =>

        // do something with arguments ...

        Done
      }
    }
  // #event-consumer
  }

  // #lambda-consumer
  val subscription: Future[Subscription] =
    session.flatMap { implicit s =>
      subscribe("mytopic", (name: String, age: Int) => {

        // do something with arguments ...
      })
    }
  // #lambda-consumer


  // #unsubscribe
  val unsubscribed: Future[Unsubscribed] =
    subscription.flatMap(s => s.unsubscribe())
  // #unsubscribe


  // ~~~~~~~~~~~~~~~~~~~~~~~~~~~

  // #call
  val result: Future[Result] =
    session.flatMap(s => s.call("myprocedure", List("paolo", 99)))
  // #call

  // #result
  result.onComplete {
    case Success(res) =>
      log.info("Result is {}", res)
    case Failure(ex) =>
      log.error(ex.getMessage, ex)
  }
  // #result

  // #register
  // val handler: (Invocation) => Future[Payload] = ...
  val registration: Future[Registration] =
    session.flatMap(s => s.register("myprocedure", handler))
  // #register

  {
  // #lambda-handler
  val registration: Future[Registration] =
    session.flatMap { implicit s =>
      register("myprocedure", (name: String, age: Int) => {

        // do something with arguments ...

      })
    }
  // #lambda-handler
  }

  // #invocation-handler
  val handler: (Invocation) => Future[Payload] =
    (invocation) => {
      val registrationId = invocation.registrationId
      val details = invocation.details
      invocation.args.map ( args => {

        // do something with arguments ...

        val res = ???
        Payload(List(res))
      })
    }
  // #invocation-handler




  // #registration
  registration.onComplete {
    case Success(sb) =>
      log.info(s"Registered ${sb.procedure} with ${sb.id}")
    case Failure(ex) =>
      log.error(ex.getMessage, ex)
  }
  // #registration

  // #unregister
  val unregistered: Future[Unregistered] =
    registration.flatMap(s => s.unregister())
  // #unregister

  val conveyor: Event = null
  // #incoming-payload
  // val conveyor: Event = ...

  conveyor.payload match {
    case p: TextLazyPayload =>
      val unparsed: Source[String, _] = p.unparsed
      // parse textual source ...

    case p: BinaryLazyPayload =>
      val unparsed: Source[ByteString, _] = p.unparsed
      // parse binary source ...
  }
  // #incoming-payload


  // #incoming-arguments
  // val conveyor: Event = ...

  val args: Future[List[Any]] = conveyor.args
  val kwargs: Future[Map[String, Any]] = conveyor.kwargs
  val user: Future[UserType] = conveyor.kwargs[UserType]
  
  class UserType(val name: String, val age: Int /*, ... */)
  // #incoming-arguments

  // #outgoing-arguments
  // list of indexed arguments
  Payload(args = List("value", 2, true /* ... */))
  
  // dictionary of named arguments
  Payload(kwargs = Map(
    "name1" -> "value1", 
    "name2" -> 2
    // ...
  ))
  // #outgoing-arguments
  



  // #close
  val close: Future[Closed] = session.flatMap(s => s.close())
  // #close

  // #disconnect
  val disconnected: Future[Disconnected] = conn.flatMap(c => c.disconnect())
  // #disconnect

  // #all-together
  // TBD
  // #all-together
  
  // #client
  // ...
}
// #client
