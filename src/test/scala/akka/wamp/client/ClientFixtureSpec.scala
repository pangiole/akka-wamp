package akka.wamp.client

import akka.actor.ActorSystem
import akka.io.IO
import akka.testkit.{TestActorRef, TestProbe}
import akka.wamp.Wamp.{Bind, Bound}
import akka.wamp.router.Router
import akka.wamp.{ActorSpec, Scope, Wamp}
import org.scalatest.ParallelTestExecution
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.duration._

class ClientFixtureSpec(_system: ActorSystem = ActorSystem("test"))
  extends ActorSpec(_system)
    with ParallelTestExecution
    with ScalaFutures {

  implicit val defaultPatience =
    PatienceConfig(timeout = 16 seconds, interval = 100 millis)

  case class FixtureParam(client: Client, router: TestActorRef[Router], listener: TestProbe, url: String)
  
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
      val theFixture = FixtureParam(Client(), router, listener, bound.url)
      withFixture(test.toNoArgTest(theFixture))
    }
    finally {
      system.stop(listener.ref)
      system.stop(router)
    }
  }
}
