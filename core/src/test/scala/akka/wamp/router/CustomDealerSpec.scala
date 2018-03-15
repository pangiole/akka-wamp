package akka.wamp.router

import akka.testkit.TestProbe
import akka.wamp.messages._

/**
  * Test a dealer configured with custom settings
  */
class CustomDealerSpec extends CustomRouterBaseSpec {

  "A dealer configured with custom settings" should "drop incoming REGISTER if peer didn't open session" in { f =>
    val client = TestProbe()
    client.send(f.router, Register(1, procedure = "myapp.procedure"))
    client.send(f.router, Hello())
    client.expectMsgType[Welcome]
    f.router.underlyingActor.sessions must have size(1)
  }


  it should "drop incoming UNREGISTER if peer didn't open session" in { f =>
    val client = TestProbe()
    client.send(f.router, Unregister(66, 99))
    client.send(f.router, Hello())
    client.expectMsgType[Welcome]
    f.router.underlyingActor.sessions must have size(1)
  }


  it should "drop incoming CALL if peer didn't open session" in { f =>
    val client = TestProbe()
    client.send(f.router, Call(444, procedure = "myapp.procedure"))
    client.send(f.router, Hello())
    client.expectMsgType[Welcome]
    f.router.underlyingActor.sessions must have size(1)
  }


  it should "drop incoming YIELD if peer didn't open session" in { f =>
    val client = TestProbe()
    client.send(f.router, Yield(1))
    client.send(f.router, Hello())
    client.expectMsgType[Welcome]
    f.router.underlyingActor.sessions must have size(1)
  }
  
  // TODO add scenarios to test the validate-strict-uris=true settings 
}
