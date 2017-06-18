package akka.wamp.serialization

import akka.actor._
import akka.stream._
import akka.stream.scaladsl._
import akka.wamp._
import org.scalatest._
import org.scalatest.concurrent._

import scala.concurrent._
import scala.concurrent.duration._

class SerializingBaseSpec 
  extends FlatSpec
    with MustMatchers
    with TryValues
    with OptionValues
    with EitherValues
    with ScalaFutures with IntegrationPatience
    with ParallelTestExecution
    with BeforeAndAfterAll
{

  implicit val defaultPatience = PatienceConfig(timeout = 32 seconds, interval = 100 millis)
  
  implicit val system = ActorSystem("test")
  implicit val materializer = ActorMaterializer()
  implicit val validator = new Validator(strictUris = false)
  implicit val ec = system.dispatcher

  override protected def afterAll(): Unit = {
    materializer.shutdown()
    system.terminate()
    Await.ready(system.whenTerminated, 32 seconds)
  }

  def single[T](x: T): Source[T, _] = Source.single(x)
}
