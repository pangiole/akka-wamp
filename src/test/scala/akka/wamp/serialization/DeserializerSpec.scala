package akka.wamp.serialization

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.wamp.Validator
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, ParallelTestExecution, _}

import scala.concurrent.Await
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

  def source[A](x: A): A = identity(x)
}
