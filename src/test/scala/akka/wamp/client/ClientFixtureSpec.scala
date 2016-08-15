package akka.wamp.client

import akka.wamp.router.RouterFixtureSpec
import org.scalatest.ParallelTestExecution
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.SpanSugar

class ClientFixtureSpec extends RouterFixtureSpec
  with ScalaFutures with SpanSugar with ParallelTestExecution {

  implicit val defaultPatience =
    PatienceConfig(timeout = 6 seconds, interval = 100 millis)

}
