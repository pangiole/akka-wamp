package akka.wamp.router

import akka.actor.ActorSystem
import akka.io.IO
import akka.testkit.{TestActorRef, TestProbe}
import akka.wamp.Wamp.{Bind, Bound}
import akka.wamp.{ActorSpec, Validator, Wamp}
import org.scalatest.{LoneElement, ParallelTestExecution}

import scala.concurrent.duration._

class RouterFixtureSpec(_system: ActorSystem = ActorSystem("test")) 
  extends ActorSpec(_system) 
    with ParallelTestExecution 
    with LoneElement
    with SequentialIdGenerators
{
  val strictUris = system.settings.config.getBoolean("akka.wamp.router.validate-strict-uris")
  
  implicit val validator = new Validator(strictUris)
  
  case class FixtureParam(router: TestActorRef[Router], client: TestProbe)

  override def withFixture(test: OneArgTest) = {
    val listener = TestProbe()
    val router = TestActorRef[Router](Router.props(scopes, Some(listener.ref)))
    try {
      IO(Wamp) ! Bind(router)
      listener.expectMsgType[Bound](16 seconds)
      val client = TestProbe("client")
      val theFixture = FixtureParam(router, client)
      withFixture(test.toNoArgTest(theFixture))
    }
    finally {
      system.stop(listener.ref)
      system.stop(router)
    }
  }
}
