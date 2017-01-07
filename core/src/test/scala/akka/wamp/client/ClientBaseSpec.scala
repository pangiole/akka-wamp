package akka.wamp.client

import java.net.URI

import akka.actor._
import akka.io.IO
import akka.testkit._
import akka.wamp.messages._
import akka.wamp._
import akka.wamp.router._
import org.scalatest._
import org.scalatest.concurrent._

import scala.concurrent.duration._

class ClientBaseSpec(_system: ActorSystem = ActorSystem("test"))
  extends ActorSpec(_system)
    with ParallelTestExecution
    with ScalaFutures {
  
  implicit val defaultPatience = PatienceConfig(timeout = 32 seconds, interval = 100 millis)

  case class FixtureParam(client: Client, router: TestActorRef[Router], uri: URI) {
    // establish a new connection to test with
    def withConnection(testCode: Connection => Unit) = {
      whenReady(client.connect(uri, "json")) { conn =>
        testCode(conn)
        // TODO conn.disconnect()
      }
    }
    // open new session to test with
    def withSession(testCode: Session => Unit): Unit = {
      withConnection { conn =>
        whenReady(conn.open()) { session =>
          testCode(session)
          session.close()
        }
      }
    }
  }

  import IdScopes._

  override def withFixture(test: OneArgTest) = {
    // TODO why following scopes are important for unit test?
    val scopes = Map(
      'global -> new SessionIdScope {},
      'router -> new SessionIdScope {},
      'session -> new SessionIdScope {}
    )
    val router = TestActorRef[Router](Router.props(scopes))
    try {
      IO(Wamp) ! Bind(router)
      val bound = expectMsgType[Bound](32 seconds)
      val client = new Client(system)
      val theFixture = FixtureParam(client, router, bound.uri)
      withFixture(test.toNoArgTest(theFixture))
    }
    finally {
      system.stop(router)
    }
  }
}
