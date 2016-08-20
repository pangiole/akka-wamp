package akka.wamp.router

import akka.actor.ActorSystem
import akka.io.IO
import akka.testkit.{TestActorRef, TestProbe}
import akka.wamp.Wamp.{Bind, Bound}
import akka.wamp.{ActorSpec, Scope, Wamp}
import org.scalatest.ParallelTestExecution
import scala.concurrent.duration._

class RouterFixtureSpec(_system: ActorSystem = ActorSystem("test")) 
  extends ActorSpec(_system) 
    with ParallelTestExecution 
{
  
  // see http://www.scalatest.org/user_guide/sharing_fixtures#withFixtureOneArgTest
  case class FixtureParam(router: TestActorRef[Router], url: String)

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
      val bound = listener.expectMsgType[Bound](6 seconds)
      val theFixture = FixtureParam(router, bound.url)
      withFixture(test.toNoArgTest(theFixture))
    }
    finally {
      system.stop(listener.ref)
      system.stop(router)
    }
  }
}
