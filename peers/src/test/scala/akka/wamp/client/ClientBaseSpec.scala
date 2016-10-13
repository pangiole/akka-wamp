package akka.wamp.client

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
  
  implicit val defaultPatience = PatienceConfig(timeout = 16 seconds, interval = 100 millis)

  case class FixtureParam(client: Client, router: TestActorRef[Router], url: String) {
    // create a new transport to test with
    def withTransport(testCode: Transport => Unit) = {
      whenReady(client.connect(url)) { transport =>
        testCode(transport)
        // TODO conn.disconnect()
      }
    }
    // create a new transport/session to test with
    def withSession(roles: Set[Role])(testCode: Session => Unit): Unit = {
      withTransport { transport =>
        whenReady(transport.openSession(roles = roles)) { session =>
          testCode(session)
          session.close()
        }
      }
    }
    // create a new transport/session to test with
    def withSession(testCode: Session => Unit): Unit = {
      withSession(Roles.client)(testCode)
    }
  }
  
  override def withFixture(test: OneArgTest) = {
    val scopes = Map(
      'global -> new Scope.SessionScope {},
      'router -> new Scope.SessionScope {},
      'session -> new Scope.SessionScope {}
    )
    val router = TestActorRef[Router](Router.props(scopes))
    try {
      IO(Wamp) ! Bind(router)
      val signal = expectMsgType[Bound](16 seconds)
      val client = new Client()(system)
      val theFixture = FixtureParam(client, router, signal.url)
      withFixture(test.toNoArgTest(theFixture))
    }
    finally {
      system.stop(router)
    }
  }
}
