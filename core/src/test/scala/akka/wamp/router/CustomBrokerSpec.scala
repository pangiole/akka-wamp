package akka.wamp.router

import akka.wamp.messages._

/**
  * Test a broker configured with custom settings
  */
class CustomBrokerSpec extends CustomRouterBaseSpec {

  "A broker configured with custom settings" should "drop incoming SUBSCRIBE if client didn't open session" in { f =>
    f.router ! Subscribe(1, topic = "mypp.topic")
    f.router ! Hello()
    expectMsgType[Welcome]
    f.router.underlyingActor.sessions must have size(1)
  }
  
  it should "drop incoming PUBLISH if client didn't open session" in { f =>
    f.router ! Publish(1, topic = "mypp.topic1")
    f.router ! Hello()
    expectMsgType[Welcome]
    f.router.underlyingActor.sessions must have size(1)
  }

  // TODO add scenarios to test the validate-strict-uris=true settings 
}
