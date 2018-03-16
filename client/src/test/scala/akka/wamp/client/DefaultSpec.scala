package akka.wamp.client

import akka.actor._
import akka.io.IO
import akka.testkit._
import akka.wamp.messages._
import akka.wamp.router.Router
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest._
import org.scalatest.concurrent._

import scala.concurrent.duration._

class DefaultSpec(_system: ActorSystem)
  extends TestKit(_system)
    with ImplicitSender
    with fixture.FlatSpecLike
    with MustMatchers
    with BeforeAndAfterAll
    with ParallelTestExecution
    with ScalaFutures with IntegrationPatience {

  def this() = this(ActorSystem(
    name = "test",
    ConfigFactory.empty()
      .withFallback(ConfigFactory.load())
  ))

  def this(config: Config) = this(ActorSystem(
    name = "test",
    config
      .withFallback(ConfigFactory.load())
  ))


  implicit val defaultPatience = PatienceConfig(timeout = 8.seconds, interval = 100.millis)

  case class FixtureParam(client: Client, router: TestActorRef[Router], uri: String) {
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


  override def withFixture(test: OneArgTest) = {
    val router = TestActorRef[Router](Props(new Router(TestIdGenerators.newTestIdGenerators)))
    try {
      IO(akka.wamp.router.Wamp) ! Bind(router, "default")
      val bound = expectMsgType[Bound](8.seconds)
      val client = Client(system)
      val theFixture = FixtureParam(client, router, bound.uri.toString)
      withFixture(test.toNoArgTest(theFixture))
    }
    finally {
      system.stop(router)
    }
  }


  override def afterAll = {
    TestKit.shutdownActorSystem(system)
  }
}
