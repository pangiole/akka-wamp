package akka.wamp.client

import akka.testkit._
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike}


abstract class BaseFlatSpec
  extends BaseSpec
  with FlatSpecLike
  with BeforeAndAfterAll {


  override def afterAll = {
    TestKit.shutdownActorSystem(system)
  }
}