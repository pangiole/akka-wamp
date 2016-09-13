package akka.wamp.router

import akka.actor.ActorSystem
import akka.io.IO
import akka.testkit.{TestActorRef, TestProbe}
import akka.wamp.Wamp.{Bind, Bound}
import akka.wamp.{ActorSpec, Validator, Wamp}
import org.scalatest.ParallelTestExecution

import scala.concurrent.duration._

class RouterFixtureSpec(_system: ActorSystem = ActorSystem("test")) 
  extends ActorSpec(_system) 
    with ParallelTestExecution 
    with SequentialIdGenerators
{
  val strictUris = system.settings.config.getBoolean("akka.wamp.serialization.validate-strict-uris")
  implicit val validator = new Validator(strictUris)
  
  case class FixtureParam(router: TestActorRef[Router], url: String)

  override def withFixture(test: OneArgTest) = {
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
