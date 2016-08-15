package akka.wamp

import akka.actor._
import akka.stream.ActorMaterializer
import akka.testkit._
import org.scalatest.{BeforeAndAfterAll, MustMatchers, ParallelTestExecution, fixture}



abstract class ActorSpec(_system: ActorSystem) 
  extends TestKit(_system)
    with ImplicitSender
    with fixture.FlatSpecLike
    with MustMatchers
    with BeforeAndAfterAll 
    with ParallelTestExecution
{
  
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

}
