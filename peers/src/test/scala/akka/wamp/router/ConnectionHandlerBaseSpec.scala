package akka.wamp.router

import akka.http.scaladsl.server._
import akka.http.scaladsl.testkit._
import akka.testkit._
import akka.wamp.Validator
import akka.wamp.serialization.JsonSerializationFlows
import org.scalatest._

/**
  * The SUT - System Under Test of this tests suite is meant to be 
  * the ``router.Connection.httpRoute``
  * 
  * Test methods of this suite are setup with fresh fixture instance
  * providing the SUT and any necessary DOC - Depends-on Components
  * Custom test doubles are NOT replacing DOCs in this tests suite,
  * but rather Akka TestKit facilities are being used.
  * 
  * httpRoute is wrapped by ``testkit.Route.seal()`` when HTTP rejections
  * need to be checked
  */
class ConnectionHandlerBaseSpec 
  extends fixture.FlatSpec 
    with MustMatchers with BeforeAndAfterAll
    with ScalatestRouteTest
    with ParallelTestExecution
    with SequentialIdGenerators
{
  val URL = "http://127.0.0.1:8080/router"
  
  def withWsClient(route: Route)(testScenario: (WSProbe) => Unit) = {
    
  }

  case class FixtureParam(httpRoute: Route, client: WSProbe)
  
  override def withFixture(test: OneArgTest) = {
    val wampRouter = TestActorRef[Router](Router.props(scopes))
    val wampClient = WSProbe()

    val routerConfig = testConfig.getConfig("akka.wamp.router")
    val path = routerConfig.getString("transport.default.path")
    val handler = TestActorRef[ConnectionHandler](ConnectionHandler.props(wampRouter, routerConfig, path))
    
    // httpRoute is the SUT - System Under Test
    val httpRoute: Route = handler.underlyingActor.httpRoute
    val theFixture = FixtureParam(httpRoute, wampClient)
    try {
      WS(URL, wampClient.flow, List("wamp.2.json")) ~> httpRoute ~> check {
        withFixture(test.toNoArgTest(theFixture))
      }
    }
    finally {
      system.stop(wampRouter)
    }
  }

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }
}