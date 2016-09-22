package akka.wamp.client

import akka.actor._
import akka.io.IO
import akka.testkit._
import akka.wamp.Wamp._
import akka.wamp._
import akka.wamp.router._
import org.scalatest._
import org.scalatest.concurrent._

import scala.concurrent.duration._

class ClientFixtureSpec(_system: ActorSystem = ActorSystem("test"))
  extends ActorSpec(_system)
    with ParallelTestExecution
    with ScalaFutures {

  
  
  implicit val defaultPatience =
    PatienceConfig(timeout = 16 seconds, interval = 100 millis)

  case class FixtureParam(client: Client, router: TestActorRef[Router], url: String) {
    // create a new connection to test with
    def withConnection(testCode: Connection => Unit) = {
      whenReady(client.connect(url)) { conn =>
        testCode(conn)
        // TODO conn.disconnect()
      }
    }
    // create a new connection/session to test with
    def withSession(roles: Set[Role])(testCode: Session => Unit): Unit = {
      withConnection { conn =>
        whenReady(conn.openSession(roles = roles)) { session =>
          testCode(session)
          session.close()
        }
      }
    }
    // create a new connection/session to test with
    def withSession(testCode: Session => Unit): Unit = {
      withSession(Roles.client)(testCode)
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
      val client = new Client()(system)
      val theFixture = FixtureParam(client, router, bound.url)
      withFixture(test.toNoArgTest(theFixture))
    }
    finally {
      system.stop(listener.ref)
      system.stop(router)
    }
  }
}
