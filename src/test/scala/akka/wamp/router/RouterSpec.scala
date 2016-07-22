package akka.wamp.router

import akka.actor.ActorSystem
import akka.io.IO
import akka.testkit.TestActorRef
import akka.wamp.Wamp.Bind
import akka.wamp.{Scope, Wamp, ActorSpec}


class RouterSpec(_system: ActorSystem) extends ActorSpec(_system) {
  
  // see http://www.scalatest.org/user_guide/sharing_fixtures#withFixtureOneArgTest
  case class FixtureParam(router: TestActorRef[Router])
  def withFixture(test: OneArgTest) = {
    val scopes = Map(
      'global -> new Scope.Session {},
      'router -> new Scope.Session {},
      'session -> new Scope.Session {}
    )
    val router = TestActorRef[Router](Router.props(scopes))
    val theFixture = FixtureParam(router)
    try {
      IO(Wamp) ! Bind(router)
      withFixture(test.toNoArgTest(theFixture)) // "loan" the fixture to the test
    }
    finally {
      system.stop(router)
    }
  }
}
