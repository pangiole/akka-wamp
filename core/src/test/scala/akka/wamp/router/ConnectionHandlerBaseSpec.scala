package akka.wamp.router

import SequentialIdGenerators.testIdGenerators
import java.net.URI

import akka.actor.Props
import akka.http.scaladsl.server._
import akka.http.scaladsl.testkit._
import akka.testkit._
import org.scalatest._

/*
  * The SUT - System Under Test of this suite is the ``ConnectionHandler.httpRoute` object!
  * 
  * Test methods are setup with a "Fresh Fixture" instance which provides both the SUT and
  * any necessary DOC - Depends-on Components
  *
  * Custom test doubles are NOT replacing DOCs in this tests suite, but rather Akka TestKit
  * facilities are being used.
  * 
  * httpRoute is wrapped by ``testkit.Route.seal()`` when HTTP rejections need to be checked
  */
class ConnectionHandlerBaseSpec 
  extends fixture.FlatSpec 
    with MustMatchers with BeforeAndAfterAll
    with ScalatestRouteTest
    with ParallelTestExecution
{
  val url = "http://127.0.0.1:8080/wamp"
  val format = "json"
  val uri = new URI(url)

  case class FixtureParam(httpRoute: Route, client: WSProbe)

  override def withFixture(test: OneArgTest) = {
    val router = TestActorRef[Router](Props(new Router(testIdGenerators)))
    val client = WSProbe()

    val handler = TestActorRef[ConnectionHandler](ConnectionHandler.props(
      router, uri, format,
      testConfig.getConfig("akka.wamp.router")
    ))

    // httpRoute is the SUT - System Under Test
    val httpRoute: Route = handler.underlyingActor.httpRoute

    val theFixture = FixtureParam(httpRoute, client)
    try {
      WS(url, client.flow, List(s"wamp.2.$format")) ~> httpRoute ~> check {
        withFixture(test.toNoArgTest(theFixture))
      }
    }
    finally {
      system.stop(router)
    }
  }

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }
}