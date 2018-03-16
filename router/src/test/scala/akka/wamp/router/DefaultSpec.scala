package akka.wamp.router

import akka.actor.Props
import akka.io.IO
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import akka.wamp.Validator
import akka.wamp.messages.{Bind, Bound, Hello, Welcome}
import akka.wamp.router.TestIdGenerators.newTestIdGenerators
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.{BeforeAndAfterAll, LoneElement, ParallelTestExecution, fixture}

import scala.concurrent.duration._

class DefaultSpec(config: Config = ConfigFactory.empty())
  extends BaseSpec(config)
    with fixture.FlatSpecLike
    with BeforeAndAfterAll
    with ImplicitSender
    with ParallelTestExecution
    with LoneElement
{

  val strictUris = system.settings.config.getBoolean("akka.wamp.router.validate-strict-uris")
  
  implicit val validator = new Validator(strictUris)
  
  case class FixtureParam(router: TestActorRef[Router], joinRealm: (String) => TestProbe)

  override def withFixture(test: OneArgTest) = {
    val router = TestActorRef[Router](Props(new Router(newTestIdGenerators)))
    try {
      IO(Wamp) ! Bind(router, endpoint = "test")
      val bound = expectMsgType[Bound](8.seconds)

      val theFixture = FixtureParam(router, (realm) => {
        val client = TestProbe("client")
        client.send(router , Hello(realm))
        client.expectMsgType[Welcome]
        client
      })
      withFixture(test.toNoArgTest(theFixture))
    }
    finally {
      system.stop(router)
    }
  }

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }
}
