package akka.wamp.client

import akka.actor.ActorSystem
import akka.io.IO
import akka.testkit.{TestActorRef, TestProbe}
import akka.wamp.Wamp.{Bind, Bound}
import akka.wamp.router.Router
import akka.wamp.{ActorSpec, Scope, Wamp}
import org.scalatest.{Outcome, ParallelTestExecution}
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.duration._

class ClientFixtureSpec(_system: ActorSystem = ActorSystem("test"))
  extends ActorSpec(_system)
    with ParallelTestExecution
    with ScalaFutures {

  implicit val defaultPatience =
    PatienceConfig(timeout = 16 seconds, interval = 100 millis)

  case class FixtureParam(router: TestActorRef[Router], url: String) {
    // create a new client to test with
    def withClient(testCode: Client => Unit) = {
      val client = Client() 
      testCode(client)
    }
    // create a new client/connection to test with
    def withConnection(testCode: Connection => Unit) = {
      withClient { client =>
        whenReady(client.connect(url)) { conn =>
          testCode(conn)
        }  
      }
    }
    // create a new client/connection/session to test with
    def withSession(testCode: Session => Unit) = {
      withConnection { conn =>
        whenReady(conn.openSession()) { session =>
          testCode(session)
        }  
      }
    }
  }
  
  override def withFixture(test: OneArgTest) = {
    val scopes = Map(
      'global -> new Scope.Session {},
      'router -> new Scope.Session {},
      'session -> new Scope.Session {}
    )
    val listener = TestProbe()
    val router = TestActorRef[Router](Router.props(scopes, Some(listener.ref)))
    try {
      IO(Wamp) ! Bind(router)
      val bound = listener.expectMsgType[Bound](16 seconds)
      val theFixture = FixtureParam(router, bound.url)
      withFixture(test.toNoArgTest(theFixture))
    }
    finally {
      system.stop(listener.ref)
      system.stop(router)
    }
  }
}
