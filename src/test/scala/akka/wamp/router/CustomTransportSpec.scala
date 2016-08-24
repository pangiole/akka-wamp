package akka.wamp.router


/**
  * It tests the ``router.Transport`` behaviour with a custom configuration 
  * (NO default settings) by exercising it with various JSON messages covering
  * the most common scenarios (session handling, subscriptions, publications, etc.) 
  * a given transport could drive into.
  */
class CustomTransportSpec extends TransportFixtureSpec {

  override def testConfigSource: String =
    """
      | akka {
      |   wamp {
      |     serialization {
      |       validate-strict-uris = true
      |     }
      |     router {
      |       abort-unknown-realms = true
      |     }
      |   }
      | }
    """.stripMargin
  
  
  "A custom router.Transport" should "handle sessions" in { fixture =>
    withHttpHandler(fixture.httpRoute) { transport =>
      
      // --> HELLOs
      transport.sendMessage("""[1,"INVALID.REALM",{"roles":{"subscriber":{}}}]""")
      transport.sendMessage("""[1,"myapp.realm",{"roles":{"subscriber":{}}}]""")

      // <-- ABORT
      transport.expectMessage("""[3,{"message":"The realm myapp.realm does not exist."},"wamp.error.no_such_realm"]""")

      // --> HELLOs
      transport.sendMessage("""[1,"akka.wamp.realm",{"roles":{"invalid":{}}}]""")
      transport.sendMessage("""[1,"akka.wamp.realm",{"roles":{"publisher":{}}}]""")

      // <-- WELCOME
      transport.expectMessage("""[2,1,{"agent":"akka-wamp-0.6.0","roles":{"broker":{}}}]""")

      // SESSION #1 OPEN

      // --> GOODBYEs
      transport.sendMessage("""[6,{},"invalid.REASON"]""")
      transport.sendMessage("""[6,{},"wamp.error.close_realm"]""")
      
      // <-- GOODBYE
      transport.expectMessage("""[6,{},"wamp.error.goodbye_and_out"]""")

      // SESSION #1 CLOSED
      // but TRANSPORT still CONNECTED :-)

      // --> HELLO
      transport.sendMessage("""[1,"akka.wamp.realm",{"roles":{"subscriber":{}}}]""")

      // <-- WELCOME
      transport.expectMessage("""[2,2,{"agent":"akka-wamp-0.6.0","roles":{"broker":{}}}]""")

      // SESSION #2 OPEN
      
      // --> GOODBYE
      transport.sendMessage("""[6,{},"wamp.error.close_realm"]""")
      
      // <-- GOODBYE
      transport.expectMessage("""[6,{},"wamp.error.goodbye_and_out"]""")

      // SESSION #2 OPEN
      // but TRANSPORT still CONNECTED :-)
      
      transport.expectNoMessage()
    }
  }
}