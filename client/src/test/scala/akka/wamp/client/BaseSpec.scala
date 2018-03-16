package akka.wamp.client

import akka.actor._
import akka.testkit._
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.MustMatchers


abstract class BaseSpec(_system: ActorSystem)
  extends TestKit(_system)
    with ImplicitSender
    with MustMatchers {

  implicit val ec = system.dispatcher


  def this() = this(ActorSystem(
    name = "test",
    ConfigFactory.empty()
      .withFallback(ConfigFactory.load())
  ))


  def this(config: Config) = this(ActorSystem(
    name = "test",
    config
      .withFallback(ConfigFactory.load())
  ))
}