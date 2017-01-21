package akka.wamp.router

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import akka.testkit.{ImplicitSender, TestActorRef, TestProbe}
import akka.wamp.messages.{Bind, Bound}
import akka.wamp.{ActorSpec, Validator, Wamp}
import SequentialIdGenerators.testIdGenerators
import org.scalatest.{LoneElement, ParallelTestExecution}

import scala.concurrent.duration._

class RouterBaseSpec(_system: ActorSystem = ActorSystem("test")) 
  extends ActorSpec(_system)
    with ImplicitSender
    with ParallelTestExecution
    with LoneElement
{
  val strictUris = system.settings.config.getBoolean("akka.wamp.router.validate-strict-uris")
  
  implicit val validator = new Validator(strictUris)
  
  case class FixtureParam(router: TestActorRef[Router], client: TestProbe)

  override def withFixture(test: OneArgTest) = {
    val router = TestActorRef[Router](Props(new Router(testIdGenerators())))
    try {
      IO(Wamp) ! Bind(router, "local")
      expectMsgType[Bound](32 seconds)
      val client = TestProbe("client")
      val theFixture = FixtureParam(router, client)
      withFixture(test.toNoArgTest(theFixture))
    }
    finally {
      system.stop(router)
    }
  }
}
