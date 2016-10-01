package akka.wamp.serialization

import akka.actor._
import akka.stream._
import akka.stream.scaladsl._
import akka.wamp._
import org.scalatest._
import org.scalatest.concurrent._

import scala.concurrent._
import scala.concurrent.duration._

class DeserializerSpec extends FlatSpec
  with MustMatchers
  with TryValues
  with OptionValues
  with EitherValues
  with ScalaFutures
  with ParallelTestExecution
  with BeforeAndAfterAll
{

  implicit val system = ActorSystem("test")
  implicit val materializer = ActorMaterializer()
  implicit val validator = new Validator(strictUris = false)

  override protected def afterAll(): Unit = {
    materializer.shutdown()
    system.terminate()
    Await.ready(system.whenTerminated, 10 seconds)
  }

  def source[T](x: T): Source[T, _] = Source.single(x)
}
