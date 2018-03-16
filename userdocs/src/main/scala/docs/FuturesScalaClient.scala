package docs


// #client
import akka.actor._
import akka.wamp.client._

// #client

import akka.stream.scaladsl._
import akka.util._
import akka.wamp.messages._
import akka.wamp.serialization._
import scala.util._


// #client
class FuturesScalaClient {
  val system = ActorSystem()
  val client = Client(system)
  // #client
  
  implicit val executionContext = system.dispatcher
  val log = system.log

  // #connect
  import scala.concurrent.Future
  val conn: Future[Connection] = client.connect("myendpoint")
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
    val consumer: (Event => Unit) = ???
    // #subscribe
    // val consumer: (Event) => Unit  = ...
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
  var freeVar: Long = 0
  val consumer: (Event) => Unit =
    event => {
      val publicationId = event.publicationId
      val subscriptionId = event.subscriptionId
      val details = event.details
      val args = event.args
      val kwargs = event.kwargs

      // so something with incoming args and free vars ...
    }
  // #event-consumer
  }

  // #lambda-consumer
//  import akka.wamp.macros._
  val subscription: Future[Subscription] = null
//    session.flatMap { implicit s =>
//      subscribe("mytopic", (name: String, age: Int) => {
//
//        // do something with arguments ...
//      })
//    }
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
  // val handler: (Invocation) => Any = ...
  val registration: Future[Registration] =
    session.flatMap(s => s.register("myprocedure", handler))
  // #register

  {
  // #lambda-handler
  val registration: Future[Registration] = null
//    session.flatMap { implicit s =>
//      register("myprocedure", (name: String, age: Int) => {
//
//        // do something with arguments ...
//
//      })
//    }
  // #lambda-handler
  }

  // #invocation-handler
  val handler: (Invocation) => Any =
    (invocation) => {
      val registrationId = invocation.registrationId
      val details = invocation.details
      val args = invocation.args
      val kwargs = invocation.kwargs

      // do something with arguments ...

      val res = ???
      res
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
  // Data conveyors are messages such as events, invocations, errors, etc.

  val args: List[Any] = conveyor.args
  val kwargs: Map[String, Any] = conveyor.kwargs
  val user: UserType = conveyor.kwargs[UserType]
  
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
